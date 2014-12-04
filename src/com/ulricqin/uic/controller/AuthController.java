package com.ulricqin.uic.controller;

import java.util.Date;

import javax.servlet.http.Cookie;

import com.jfinal.core.Controller;
import com.ulricqin.frame.exception.RenderJsonMsgException;
import com.ulricqin.frame.kit.Checker;
import com.ulricqin.frame.kit.LDAP;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.config.Config;
import com.ulricqin.uic.model.Session;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.UserService;
import com.unboundid.ldap.sdk.LDAPException;

public class AuthController extends Controller {

	public void login() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			loginGet();
		} else {
			loginPost();
		}
	}

	private void loginGet() {
		String appSig = getPara("sig", "");
		String callback = getPara("callback", "");
		if (StringKit.isNotBlank(appSig) && StringKit.isNotBlank(callback)) {
			ssoLogin(appSig, callback);
			return;
		}

		User u = parseUserFromCookie();
		if (u == null) {
			renderLoginPage("", "");
			return;
		}

		redirect("/");
	}

	private void ssoLogin(String appSig, String callback) {
		String cookieSig = getCookie("sig");
		if (StringKit.isBlank(cookieSig)) {
			// not login
			renderLoginPage(appSig, callback);
			return;
		}

		User u = UserService.getBySig(cookieSig);
		if (u == null) {
			// not login
			renderLoginPage(appSig, callback);
			return;
		}

		// has login
		if (!attachSession(cookieSig, appSig)) {
			throw new RenderJsonMsgException("occur unknown error");
		}

		if (StringKit.isBlank(callback)) {
			callback = "/";
		}

		redirect(callback);
	}

	private boolean attachSession(String cookieSig, String appSig) {
		Session s = Session.dao.getBySig(cookieSig);
		if (s == null) {
			return false;
		}

		Session newSession = new Session();
		newSession.set("uid", s.getLong("uid"));
		newSession.set("expired", s.getLong("expired"));
		newSession.set("sig", appSig);
		return newSession.save();
	}

	private void renderLoginPage(String appSig, String callback) {
		setAttr("ldapEnabled", Config.ldapEnabled);
		setAttr("sig", appSig);
		setAttr("callback", callback);
		setAttr("title", "sign in");
		render("login.html");
	}

	private User parseUserFromCookie() {
		String cookieSig = getCookie("sig");
		if (StringKit.isBlank(cookieSig)) {
			return null;
		}

		return UserService.getBySig(cookieSig);
	}

	private void loginPost() {
		String name = getPara("name", "");
		String password = getPara("password", "");

		User targetUser = null;

		if (Config.ldapEnabled) {
			if (StringKit.isBlank(name)) {
				throw new RenderJsonMsgException("name is blank");
			}

			try {
				boolean successfully = new LDAP().auth(name, password);
				if (!successfully) {
					throw new RenderJsonMsgException("name or password error");
				}

				// LDAP check successfully
				Long id = UserService.getIdByName(name);
				if (id == null) {
					// no such user in database
					targetUser = new User();
					targetUser.set("name", name);
					targetUser.set("passwd", "ldap");
					if (!targetUser.save()) {
						throw new RenderJsonMsgException("occur unknown error");
					}
				}

				targetUser = UserService.getByName(name);
			} catch (LDAPException e) {
				throw new RenderJsonMsgException("LDAP ERROR: "
						+ e.getMessage());
			}
		} else {
			if (StringKit.isBlank(name) || StringKit.isBlank(password)) {
				throw new RenderJsonMsgException("name or password is blank");
			}

			// can't use UserService.getByName(). cause the user it returned
			// don't
			// have password
			targetUser = User.dao.findFirst(
					"select id,name,passwd from user where name = ?", name);
			if (targetUser == null) {
				throw new RenderJsonMsgException("no such user");
			}

			if (!targetUser.getStr("passwd").equals(password)) {
				throw new RenderJsonMsgException("password error");
			}
		}

		Cookie cookie = genCookie(targetUser);
		setCookie(cookie);

		String appSig = getPara("sig", "");
		String callback = getPara("callback", "");
		if (StringKit.isNotBlank(appSig) && StringKit.isNotBlank(callback)) {
			if (!attachSession(cookie.getValue(), appSig)) {
				throw new RenderJsonMsgException("occur unknown error");
			}
		}
		setAttr("msg", "");
		setAttr("callback", callback);
		renderJson();
	}

	public void logout() {
		setCookie("sig", "", 0, "/");
		redirect("/");
	}

	public void register() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			registerGet();
		} else {
			registerPost();
		}
	}

	public void registerGet() {
		boolean canRegister = Config.canRegister;
		if (Config.ldapEnabled) {
			canRegister = false;
		}
		setAttr("canRegister", canRegister);
		setAttr("title", "sign up");
		render("register.html");
	}

	public void registerPost() {
		if (!Config.canRegister) {
			throw new RenderJsonMsgException("registration system is not open");
		}

		String name = getPara("name", "");
		String password = getPara("password", "");
		String repeatPassword = getPara("repeat_password", "");

		if (!password.equals(repeatPassword)) {
			throw new RenderJsonMsgException("repeat_password != password");
		}

		if (!Checker.isUserNameValid(name)) {
			throw new RenderJsonMsgException("name is invalid. use a-z0-9_");
		}

		Long id = UserService.getIdByName(name);
		if (id != null) {
			throw new RenderJsonMsgException("name is already existent");
		}

		User newUser = new User();
		newUser.set("name", name);
		newUser.set("passwd", password);
		if (!newUser.save()) {
			throw new RenderJsonMsgException("occur unknown error");
		}

		setCookie(genCookie(newUser));
		renderJson("msg", "");
	}

	private Cookie genCookie(User u) {
		Long uid = u.getLong("id");
		int maxAge = 3600 * 24 * 30;

		String sig = genSig(uid, maxAge);
		Cookie cookie = new Cookie("sig", sig);
		cookie.setPath("/");

		cookie.setMaxAge(maxAge);
		cookie.setHttpOnly(true);
		return cookie;
	}

	private String genSig(Long uid, int maxAge) {
		String sig = StringKit.randomStr32();
		Session session = new Session();
		session.set("uid", uid);
		session.set("sig", sig);
		session.set("expired", new Date().getTime() / 1000 + maxAge);
		if (!session.save()) {
			throw new RenderJsonMsgException("save session fail");
		}
		return sig;
	}

}

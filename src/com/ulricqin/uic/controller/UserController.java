package com.ulricqin.uic.controller;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.jfinal.aop.Before;
import com.jfinal.aop.ClearInterceptor;
import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.ulricqin.frame.exception.RenderJsonMsgException;
import com.ulricqin.frame.kit.Checker;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.frame.kit.ZxingKit;
import com.ulricqin.uic.config.Config;
import com.ulricqin.uic.interceptor.LoginRequiredInterceptor;
import com.ulricqin.uic.interceptor.MakeSureTargetUserExistsInterceptor;
import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.TeamService;
import com.ulricqin.uic.service.UserService;

@Before(LoginRequiredInterceptor.class)
public class UserController extends Controller {

	public void index() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			indexGet();
		} else {
			indexPost();
		}
	}

	public void indexGet() {
		String p = getPara();
		if (StringKit.isBlank(p)) {
			getResponse().setStatus(400);
			renderNull();
			return;
		}
		renderText("not implemented indexGet, parameter: " + p);
	}

	public void indexPost() {
		renderText("not implemented indexPost");
	}

	// 自己更新自己
	public void update() {
		String cnname = getPara("cnname", "");
		cnname = Jsoup.clean(cnname, Whitelist.none());
		String email = getPara("email", "");
		email = Jsoup.clean(email, Whitelist.none());
		String phone = getPara("phone", "");
		phone = Jsoup.clean(phone, Whitelist.none());
		String im = getPara("im", "");
		im = Jsoup.clean(im, Whitelist.none());
		String qq = getPara("qq", "");
		qq = Jsoup.clean(qq, Whitelist.none());

		User me = getAttr("me");
		boolean success = me.updateProfile(cnname, email, phone, im, qq);
		if (success) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "没有更新任何字段");
		}
	}

	// 我去更新别人的信息
	@Before(MakeSureTargetUserExistsInterceptor.class)
	public void edit() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			editGet();
		} else {
			editPost();
		}
	}

	private void editGet() {
		User toEdit = getAttr("targetUser");
		setAttr("user", toEdit);
		setAttr("title", "编辑用户");
		setAttr("isUserPage", true);
	}

	private void editPost() {
		String cnname = getPara("cnname", "");
		cnname = Jsoup.clean(cnname, Whitelist.none());
		String email = getPara("email", "");
		email = Jsoup.clean(email, Whitelist.none());
		String phone = getPara("phone", "");
		phone = Jsoup.clean(phone, Whitelist.none());
		String im = getPara("im", "");
		im = Jsoup.clean(im, Whitelist.none());
		String qq = getPara("qq", "");
		qq = Jsoup.clean(qq, Whitelist.none());

		User toEdit = getAttr("targetUser");

		User me = getAttr("me");
		if (!me.canWrite(toEdit)) {
			renderJson("msg", "no privilege");
			return;
		}

		boolean success = toEdit.updateProfile(cnname, email, phone, im, qq);
		if (success) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "没有更新任何字段");
		}
	}

	public void chpwd() {
		String oldPassword = getPara("old_password", "");
		String newPassword = getPara("new_password", "");
		String repeatPassword = getPara("repeat_password", "");

		User me = getAttr("me");
		// me中没有passwd，重新从db获取
		me = User.dao.findById(me.getLong("id"), "id,name,passwd");
		if (!me.getStr("passwd").equals(oldPassword)) {
			renderJson("msg", "旧密码错误");
			return;
		}

		if (!newPassword.equals(repeatPassword)) {
			renderJson("msg", "输入的两次新密码不一致");
			return;
		}

		if (me.getStr("passwd").equals(newPassword)) {
			renderJson("msg", "");
			return;
		}

		boolean success = me.updatePasswd(newPassword);
		if (success) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "密码修改失败，请稍候重试");
		}
	}

	public void all() {
		setAttr("title", "User列表");
		setAttr("isUserPage", true);

		String query = getPara("q", "");
		query = Jsoup.clean(query, Whitelist.none());
		int iAmCreator = getParaToInt("iamcreator", 0);
		int pageNo = getParaToInt(0, 1);
		User me = getAttr("me");

		setAttr("query", query);
		setAttr("iamcreator", iAmCreator);
		setAttr("userPage",
				User.dao.paginate(pageNo, 10, query, iAmCreator > 0, me));
	}

	public void create() {
		String method = getRequest().getMethod();
		if (method.equalsIgnoreCase("GET")) {
			createGet();
		} else {
			createPost();
		}
	}

	private void createGet() {
		setAttr("title", "创建新用户");
		setAttr("isUserPage", true);
	}

	private void createPost() {
		String name = getPara("name", "");
		name = Jsoup.clean(name, Whitelist.none());
		if (StringKit.isBlank(name)) {
			renderJson("msg", "name is blank");
			return;
		}
		
		if (!Checker.isIdentifier(name)) {
			throw new RenderJsonMsgException("name is invalid. use a-zA-Z0-9-_");
		}

		User u = UserService.getByName(name);
		if (u != null) {
			renderJson("msg", "用户名已经被占用，换一个呗");
			return;
		}

		String cnname = getPara("cnname", "");
		cnname = Jsoup.clean(cnname, Whitelist.none());
		String email = getPara("email", "");
		email = Jsoup.clean(email, Whitelist.none());
		String phone = getPara("phone", "");
		phone = Jsoup.clean(phone, Whitelist.none());
		String passwd = getPara("password", "");
		
		User me = getAttr("me");

		User newUser = new User();
		newUser.set("name", name);
		newUser.set("passwd", passwd);
		newUser.set("cnname", cnname);
		newUser.set("email", email);
		newUser.set("phone", phone);
		newUser.set("creator", me.getLong("id"));
		if (!newUser.save()) {
			renderJson("msg", "出现未知错误，数据库可能宕了，请联系管理员");
			return;
		}

		renderJson("msg", "");
	}

	@Before(MakeSureTargetUserExistsInterceptor.class)
	public void delete() {
		User toDelete = getAttr("targetUser");

		User me = getAttr("me");
		if (!me.canWrite(toDelete)) {
			renderJson("msg", "no privilege");
			return;
		}

		if (UserService.deleteUser(toDelete)) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "occur unknown error");
		}
	}

	@ClearInterceptor
	@ActionKey("/about")
	public void about() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String name = getPara();
		if (StringKit.isBlank(name)) {
			// 最上层有个global intercepter统一处理exception并且把msg render json了
			// 所以controller其实可以直接抛异常就好
			throw new RenderJsonMsgException("name is blank");
		}

		if (name.endsWith("@xiaomi.com")) {
			name = name.substring(0, name.length() - 11);
		}

		User u = UserService.getByName(name);
		if (u == null) {
			throw new RenderJsonMsgException("no such user");
		}

		setAttr("user", u);
	}

	@ActionKey("/qrcode")
	public void qrcode() {
		int id = getParaToInt();
		if (id == 0) {
			throw new RenderJsonMsgException("id is blank");
		}

		User u = UserService.getById(id);
		if (u == null) {
			throw new RenderJsonMsgException("no such user");
		}

		int width = 300, height = 300;
		File varDir = new File("var");
		if (!varDir.exists()) {
			varDir.mkdir();
		}
		File imgFile = new File("var" + File.separator + u.getLong("id")+".png");
		ZxingKit.encode("BEGIN:VCARD\nVERSION:3.0\nFN:" + u.getStr("cnname")
				+ "\nTEL;WORK;VOICE:" + u.getStr("phone")
				+ "\nEMAIL;PREF;INTERNET:" + u.getStr("email")
				+ "\nORG:WORK\nEND:VCARD", width, height, imgFile);
		renderFile(imgFile);
	}

	// 在页面上对某个人点击放大镜按钮查看详情的时候要有个弹层
	@Before(MakeSureTargetUserExistsInterceptor.class)
	public void detail() {
		User u = getAttr("targetUser");
		setAttr("user", u);
		render("../common/_about.html");
	}
	
	public void query() {
		String query = getPara("query", "");
		query = Jsoup.clean(query, Whitelist.none());
		int limit = getParaToInt("limit", 10);
		renderJson("users", User.dao.query(query, limit));
	}
	
	@ClearInterceptor
	public void in() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		String userName = getPara("name", "");
		String teams = getPara("teams", "");
		if (userName.equals("") || teams.equals("")) {
			renderText("0");
			return;
		}
		
		String[] teamArr = StringUtils.split(teams, ",;");
		for (int i = 0; i < teamArr.length; i++) {
			// check every team
			Team t = TeamService.getByName(teamArr[i]);
			if (t == null) {
				continue;
			}
			
			List<User> users = t.getUsers();
			for (User user : users) {
				if (user.getStr("name").equals(userName)) {
					renderText("1");
					return;
				}
			}
		}
		
		renderText("0");
	}
	
	@ClearInterceptor
	public void names() {
		if (!getPara("token", "").equals(Config.token)) {
			renderText("no privilege");
			return;
		}
		
		List<Record> rs = Db.find("select name from user");
		List<String> names = new LinkedList<String>();
		for (Record r : rs) {
			names.add(r.getStr("name"));
		}
		
		setAttr("names", names);
		setAttr("msg", "");
		renderJson();
	}
}

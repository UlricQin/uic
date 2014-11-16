package com.ulricqin.uic.controller;

import com.jfinal.core.Controller;
import com.ulricqin.frame.kit.StringKit;
import com.ulricqin.uic.config.Config;
import com.ulricqin.uic.model.Session;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.UserService;

public class SsoController extends Controller {

	public void index() {
		renderText("ok");
	}
	
	public void sig() {
		renderText(StringKit.randomStr32());
	}
	
	public void user() {
		if (!getPara("token", "").equals(Config.token)) {
			getResponse().setStatus(404);
			renderText("no privilege");
			return;
		}
		
		String sig = getPara();
		if (StringKit.isBlank(sig)) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		User u = UserService.getBySig(sig);
		if (u == null) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		renderJson("user", u);
	}
	
	public void logout() {
		if (!getPara("token", "").equals(Config.token)) {
			getResponse().setStatus(404);
			renderText("no privilege");
			return;
		}
		
		String sig = getPara();
		if (StringKit.isBlank(sig)) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		User u = UserService.getBySig(sig);
		if (u == null) {
			getResponse().setStatus(404);
			renderNull();
			return;
		}
		
		Session.dao.deleteAllByUser(u);
		renderText("");
	}
	
}

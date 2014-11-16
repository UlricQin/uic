package com.ulricqin.uic.controller;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.ulricqin.uic.interceptor.AdminRequiredInterceptor;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.plugin.MemcachePlugin;
import com.ulricqin.uic.service.UserService;

@Before(AdminRequiredInterceptor.class)
public class AdminController extends Controller {

	public void index() {
		renderText("ok");
	}
	
	public void role() {
		int id = getParaToInt("id", 0);
		if (id == 0) {
			renderJson("msg", "user id is invalid");
			return;
		}

		User u = UserService.getById(id);
		if (u == null) {
			renderJson("msg", "no such user");
			return;
		}

		if (u.getStr("name").equals("root")) {
			renderJson("msg", "no privilege");
			return;
		}

		u.set("role", getParaToInt("role", 0));

		if (u.update()) {
			renderJson("msg", "");
		} else {
			renderJson("msg", "occur unknown error");
		}
	}
	
	public void clear() {
		MemcachePlugin.client.flushAll();
		renderText("ok");
	}
}

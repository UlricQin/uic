package com.ulricqin.uic.controller;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.ulricqin.uic.interceptor.LoginRequiredInterceptor;

@Before(LoginRequiredInterceptor.class)
public class MainController extends Controller {

	public void index() {
		setAttr("isProfilePage", true);
		setAttr("title", "UIC");
		render("index.html");
	}
}

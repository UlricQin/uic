package com.ulricqin.uic.controller;

import com.jfinal.core.Controller;

public class HealthController extends Controller {

	public void index() {
		renderText("ok");
	}
	
}

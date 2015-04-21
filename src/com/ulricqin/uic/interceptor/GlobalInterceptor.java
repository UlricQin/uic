package com.ulricqin.uic.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.core.ActionInvocation;

public class GlobalInterceptor implements Interceptor {
	
	@Override
	public void intercept(ActionInvocation ai) {
		try {
			ai.invoke();
		} catch (Exception e) {
			if (!"name or password error".equals(e.getMessage())) {
				// TODO: send stackTrace to maintainer(email)
				System.out.println(">>> " + e.getMessage());
				e.printStackTrace();
			}
			ai.getController().renderJson("msg", e.getMessage());
		}
	}

}

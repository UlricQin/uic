package com.ulricqin.uic.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.core.ActionInvocation;
import com.jfinal.core.Controller;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.UserService;

public class LoginRequiredInterceptor implements Interceptor {

	@Override
	public void intercept(ActionInvocation ai) {
		Controller controller = ai.getController();
		String sig = controller.getCookie("sig");
		if (sig == null || sig.equals("")) {
			controller.redirect("/auth/login");
			return;
		}
		
		User u = UserService.getBySig(sig);
		if (u == null) {
			controller.redirect("/auth/login");
			return;
		}
		
		if (u.getInt("role") < 0) {
			// be blocked
			controller.redirect("/auth/login");
			return;
		}
		
		controller.setAttr("me", u);
		ai.invoke();
	}
	
}

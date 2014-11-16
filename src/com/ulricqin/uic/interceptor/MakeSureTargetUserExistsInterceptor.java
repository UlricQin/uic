package com.ulricqin.uic.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.core.ActionInvocation;
import com.jfinal.core.Controller;
import com.ulricqin.uic.model.User;
import com.ulricqin.uic.service.UserService;

public class MakeSureTargetUserExistsInterceptor implements Interceptor {
	
	@Override
	public void intercept(ActionInvocation ai) {
		Controller controller = ai.getController();
		int id = controller.getParaToInt("id", 0);
		if (id == 0) {
			controller.renderJson("msg", "id is blank");
			return;
		}

		User targetUser = UserService.getById(id);
		if (targetUser == null) {
			controller.renderJson("msg", "no such user");
			return;
		}
		
		controller.setAttr("targetUser", targetUser);
		
		ai.invoke();
	}

}

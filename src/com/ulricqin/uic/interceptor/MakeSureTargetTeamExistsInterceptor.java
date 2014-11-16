package com.ulricqin.uic.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.core.ActionInvocation;
import com.jfinal.core.Controller;
import com.ulricqin.uic.model.Team;
import com.ulricqin.uic.service.TeamService;

public class MakeSureTargetTeamExistsInterceptor implements Interceptor {
	
	@Override
	public void intercept(ActionInvocation ai) {
		Controller controller = ai.getController();
		int id = controller.getParaToInt("id", 0);
		if (id == 0) {
			controller.renderJson("msg", "id is blank");
			return;
		}

		Team target = TeamService.getById(id);
		if (target == null) {
			controller.renderJson("msg", "no such team");
			return;
		}
		
		controller.setAttr("targetTeam", target);
		
		ai.invoke();
	}

}

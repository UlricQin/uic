package com.ulricqin.uic.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.ulricqin.uic.plugin.MemcachePlugin;

public class Session extends Model<Session> {

	private static final long serialVersionUID = 5484351477902130545L;
	public static final Session dao = new Session();

	public Session getBySig(String sig) {
		String key = "s:" + sig;
		Session s = (Session) MemcachePlugin.client.get(key);
		if (s == null) {
			s = findFirst("select * from `session` where `sig`=?", sig);
			if (s == null) {
				return null;
			}
			MemcachePlugin.client.set(key, s);
		}
		return s;
	}
	
	public void deleteBySig(String sig) {
		int rowCnt = Db.update("delete from `session` where `sig` = ?", sig);
		if (rowCnt > 0) {
			MemcachePlugin.client.delete("s:" + sig);
		}
	}

	public void deleteAllByUser(User u) {
		List<Session> ss = dao.find("select * from `session` where uid = ?", u.getLong("id"));
		for (Session s : ss) {
			deleteBySig(s.getStr("sig"));
		}
	}
}

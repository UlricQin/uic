function err_message_quietly(msg, f) {
	$.layer({
		title : false,
		closeBtn : false,
		time : 2,
		dialog : {
			msg : msg
		},
		end : f
	});
}

function ok_message_quietly(msg, f) {
	$.layer({
		title : false,
		closeBtn : false,
		time : 1,
		dialog : {
			msg : msg,
			type : 1
		},
		end : f
	});
}

function my_confirm(msg, btns, yes_func, no_func) {
	$.layer({
		shade : [ 0 ],
		area : [ 'auto', 'auto' ],
		dialog : {
			msg : msg,
			btns : 2,
			type : 4,
			btn : btns,
			yes : yes_func,
			no : no_func
		}
	});
}

// - business function -

function update_profile() {
	$.post('/user/update', {
		'cnname' : $("#cnname").val(),
		'email' : $("#email").val(),
		'phone' : $("#phone").val(),
		'im' : $("#im").val(),
		'qq' : $("#qq").val()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly("更新成功：）");
		}
	});
}

function edit_user(id) {
	$.post('/user/edit', {
		'cnname' : $("#cnname").val(),
		'email' : $("#email").val(),
		'phone' : $("#phone").val(),
		'im' : $("#im").val(),
		'qq' : $("#qq").val(),
		'id' : id
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly("更新成功：）");
		}
	});
}

function change_password() {
	$.post('/user/chpwd', {
		'old_password' : CryptoJS.MD5($("#old_password").val()).toString(),
		'new_password' : CryptoJS.MD5($("#new_password").val()).toString(),
		'repeat_password' : CryptoJS.MD5($("#repeat_password").val())
				.toString()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly("密码修改成功：）");
		}
	});
}

function register() {
	$.post('/auth/register', {
		'name' : $('#name').val(),
		'password' : CryptoJS.MD5($("#password").val()).toString(),
		'repeat_password' : CryptoJS.MD5($("#repeat_password").val())
				.toString()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly('sign up successfully', function() {
				location.href = '/';
			});
		}
	});
}

function login(ldapEnabled) {
	var pass = $("#password").val();
	if (ldapEnabled == "false") {
		pass = CryptoJS.MD5(pass).toString();
	}
	$.post('/auth/login', {
		'name' : $('#name').val(),
		'password' : pass,
		'sig': $("#sig").val(),
		'callback': $("#callback").val()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly('sign in successfully', function() {
				var redirect_url = '/';
				if (json.callback.length > 0) {
					redirect_url = json.callback;
				}
				location.href = redirect_url;
			});
		}
	});
}

function query_user() {
	var iamcreator = document.getElementById("iamcreator").checked ? '1' : '0';
	var query = $("#query").val();
	location.href = "/user/all/1?q=" + query + "&iamcreator=" + iamcreator;
}

function query_team() {
	var query = $("#query").val();
	location.href = "/team/all/1?q=" + query;
}

function create_user() {
	$.post('/user/create', {
		'name' : $("#name").val(),
		'cnname' : $("#cnname").val(),
		'email' : $("#email").val(),
		'phone' : $("#phone").val(),
		'password' : CryptoJS.MD5("abc").toString()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly('create user successfully', function() {
				$("#name").val("");
				$("#cnname").val("");
				$("#email").val("");
				$("#phone").val("");
			});
		}
	});
}

function create_team() {
	$.post('/team/create', {
		'name' : $("#name").val(),
		'resume' : $("#resume").val(),
		'users' : $("#users").val()
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly('create team successfully');
		}
	});
}

function edit_team(tid) {
	$.post('/team/edit', {
		'resume' : $("#resume").val(),
		'users' : $("#users").val(),
		'id': tid
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
		} else {
			ok_message_quietly('edit team successfully');
		}
	});
}

function delete_user(uid) {
	my_confirm("真的真的要删除么？", [ '确定', '取消' ], function() {
		$.post('/user/delete', {
			'id' : uid
		}, function(json) {
			if (json.msg.length > 0) {
				err_message_quietly(json.msg);
			} else {
				ok_message_quietly('delete user successfully', function() {
					location.reload();
				});
			}
		});
	}, function() {
	});
}

function delete_team(id) {
	my_confirm("真的真的要删除么？", [ '确定', '取消' ], function() {
		$.post('/team/delete', {
			'id' : id
		}, function(json) {
			if (json.msg.length > 0) {
				err_message_quietly(json.msg);
			} else {
				ok_message_quietly('delete team successfully', function() {
					location.reload();
				});
			}
		});
	}, function() {
	});
}

function set_role(uid, obj) {
	var role = obj.checked ? '1' : '0';
	$.post('/admin/role', {
		'id' : uid,
		'role' : role
	}, function(json) {
		if (json.msg.length > 0) {
			err_message_quietly(json.msg);
			location.reload();
		} else {
			if (role == '1') {
				ok_message_quietly('成功设置为管理员：）');
			} else if (role == '0') {
				ok_message_quietly('成功取消管理员权限：）');
			}
		}
	});
}

function user_detail(uid) {
	$("#user_detail_div").load("/user/detail?id=" + uid);
	$.layer({
		type : 1,
		shade : [ 0.5, '#000' ],
		shadeClose : true,
		closeBtn : [ 0, true ],
		area : [ '450px', '240px' ],
		title : false,
		border : [ 0 ],
		page : {
			dom : '#user_detail_div'
		}
	});
}

/**
 * Created by xpf on 2018/10/30
 * 图片验证码
 */
(function ($) {
    $.fn.imgcode = function (options) {
        //初始化参数
        var defaults = {
            callback:""  //回调函数
        };
        var opts = $.extend(defaults, options);
        return this.each(function () {
            var $this = $(this);//获取当前对象
			var html = '<div class="code-k-div">' +
                '<div class="code_bg"></div>' +
                '<div class="code-con">' +
                '<div class="code-img">' +
                '<div class="code-img-con">' +
                '<div class="code-mask"><img id= "puzzleFront" src="' + options.target + '" height="150" width="70"></div>' +
                '<img id="puzzleBack" src="' + options.original + '" height="150" width="300"></div>' +
                '<div class="code-push">' +
                // '<i class="icon-login-bg icon-w-25 icon-push">刷新</i>' +
                '<span class="code-tip"></span></div>' +
                '</div>' +
                '<div class="code-btn">' +
                '<div class="code-btn-img code-btn-m"></div>' +
                '<span id="codeBtnTip" style="padding-left: 10px;">&gt;&gt;&nbsp;按住滑块，拖动完成上方拼图</span>' +
                '</div></div></div>';
            $this.html(html);

            //定义拖动参数
            var $divMove = $(this).find(".code-btn-img"); //拖动按钮
            var $divWrap = $(this).find(".code-btn");//按钮可滑动区域
            var $divImage = $(this).find(".code-img");//验证图片区域
            var $divContain = $("#contain");//验证图片区域
            var mX = 0, mY = 0;//定义鼠标X轴Y轴
            var dX = 0, dY = 0;//定义滑动区域左、上位置
            var isDown = false;//mousedown标记
            if(document.attachEvent) {//ie的事件监听，拖拽div时禁止选中内容，firefox与chrome已在css中设置过-moz-user-select: none; -webkit-user-select: none;
                $divMove[0].attachEvent('onselectstart', function() {
                    return false;
                });
            }
            $divWrap.on({
                mouseover:function(e){
                    if (!$divMove.hasClass("code-btn-green")) {
                        $divImage.show();
                    }
                },
                mouseout:function(e){
                    if (!$divMove.hasClass("active")) {
                        //$divImage.hide();
                    }
                }
            });
            //按钮拖动事件
            $divMove.on({
                mousedown: function (e) {
                    //按钮不为绿色时，可以拖动
                    if (!$divMove.hasClass("code-btn-green")) {
                        //拖动时隐藏按钮提示文字
                        $("#codeBtnTip").fadeOut(600);
                        //清除提示信息
                        $this.find(".code-tip").html("");
                        var event = e || window.event;
                        mX = event.pageX;
                        dX = $divWrap.offset().left;
                        dY = $divWrap.offset().top;
                        //启动鼠标拖拽
                        isDown = true;
                        $(this).addClass("active");
                        //修改按钮阴影
                        $divMove.css({"box-shadow":"0 0 8px #666"});
                    }
                }
            });
            //鼠标点击松手事件
            $(document).mouseup(function (e) {
                //鼠标拖拽结束时调用验证
                if (isDown) {
                    var lastX = $this.find(".code-mask").offset().left - dX - 1;
                    isDown = false;//鼠标拖拽启
                    $divMove.removeClass("active");
                    //还原按钮阴影
                    $divMove.css({"box-shadow":"0 0 3px #ccc"});
                    checkcode(lastX);
                }
                if (!$divWrap.is(":hover")) {
                    //$divImage.hide();
                }
            });
            //滑动事件
            $divContain.mousemove(function (event) {
                if (isDown) {
                    var event = event || window.event;
                    var x = event.pageX;//鼠标滑动时的X轴
                    if(x > (dX + 30) && x < dX + $divImage.width() - 40){
                        $divMove.css({"left": (x - dX - 20) + "px"});//div动态位置赋值
                        $this.find(".code-mask").css({"left": (x - dX-30) + "px"});
                    }
                }
            });

            //验证数据
            function checkcode(code){
                //模拟ajax请求后台验证后获取的结果
				var x = 220.5;
				var result = new Object();
				if (code - x < 5 && x - code < 5) {
					result.code = 0;
				} else {
					result.code = 1;
				}
				if(result.code == 0){
					checkcoderesult(1,"验证通过");
					//记录当前的滑动，登录时还会校验
					$("#code").val(result.data);
					//验证结果延迟显示
					setTimeout(function(){
						//隐藏弹窗，也同时隐藏验证提示
						//$this.find(".code-k-div").hide();
						//checkcoderesult(1,"");
						// opts.callback({code:1000,msg:"验证通过",msgcode:"23dfdf123"});
						//$("#bg").show();
						location.reload();
					},500);
				} else {
					$("#code").val("");
					//验证结果延迟显示
					setTimeout(function(){
						$divMove.addClass("error");
						checkcoderesult(0,"验证不通过");
						// opts.callback({code:1001,msg:"验证不通过"});
						//重新显示按钮提示文字
						$("#codeBtnTip").fadeIn(1000);
						setTimeout(function() {
							$divMove.removeClass("error");
							$this.find(".code-mask").animate({"left":"0px"},200);
							$divMove.animate({"left": "10px"},200);
							//更新验证图片
							//$("#puzzleFront").attr("src",result.data.target);
							//$("#puzzleBack").attr("src",result.data.original);
							$this.find(".code-btn-m").addClass("code-btn-white");
							$this.find(".code-btn-m").removeClass("code-btn-red");
						},500);
					},100);
				}
            }

            //验证结果
            function checkcoderesult(i,txt){
                if(i == 0){
                    $this.find(".code-tip").addClass("code-tip-red");
                    $this.find(".code-btn-m").removeClass("code-btn-white");
                    $this.find(".code-btn-m").removeClass("code-btn-green");
                    $this.find(".code-btn-m").addClass("code-btn-red");
                } else {
                    $this.find(".code-tip").addClass("code-tip-green");
                    $this.find(".code-btn-m").removeClass("code-btn-white");
                    $this.find(".code-btn-m").removeClass("code-btn-red");
                    $this.find(".code-btn-m").addClass("code-btn-green");
                    //$this.find(".code-img").hide();
                }
                $this.find(".code-tip").html(txt);
            }
        })
    }
})(jQuery);
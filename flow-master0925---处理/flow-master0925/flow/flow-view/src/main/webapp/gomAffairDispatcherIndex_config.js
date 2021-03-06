/**
 * 事务调单主页require配置文件主入口
 */
require.config({
    urlArgs: 'v=20201123',
    waitSeconds:0
});
require(['frame/ext/ngc'], function (n) {
    require(["module/component/utils/LoginStatChange"], function (k) {
        require(['module/UnicomLocalNet/resmaster/portal/gomAffairDispatcher/views/main',
            'i18n!module/UnicomLocalNet/resmaster/portal/i18n/portalView.i18n'
        ], function (patchIndex, portalViewi18n) {
            this.$("#title").html(portalViewi18n.RESOURCE_MANAGEMENT_HOMEPAGE);
            new patchIndex({el: '#main_index_content'});
        })
    })
});

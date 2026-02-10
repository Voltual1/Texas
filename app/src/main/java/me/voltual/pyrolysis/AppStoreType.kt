//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis

enum class AppStore(val displayName: String) {
    XIAOQU_SPACE("小趣空间"),
    SIENE_SHOP("弦应用商店"),
    SINE_OPEN_MARKET("弦-开放平台"), 
    LING_MARKET("灵应用商店"), 
    // WYSAPPMARKET("微思应用商店"), // 该平台因系统性侵权及欺诈还有对本项目极大的恶意已移除，代码层面永久弃用(▼ヘ▼#)
    F_DROID("F-Droid"), // 替代原微思位置，引入开源合规生态，我们将会参考同属自由软件GPLV3许可证的哥们——F-droid客户端项目:Neo Store
    LOCAL("本地应用")
}
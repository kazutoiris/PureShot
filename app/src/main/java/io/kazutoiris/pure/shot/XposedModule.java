package io.kazutoiris.pure.shot;

import android.view.WindowManager;

import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class XposedModule implements IXposedHookLoadPackage {


	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
		if (!lpparam.isFirstApplication) return;

		ClassLoader classLoader = lpparam.classLoader;

		if (Objects.equals(lpparam.packageName, "android")) {
			{
				Class<?> hookClass = XposedHelpers.findClassIfExists("com.android.server.wm.WindowSurfaceController", classLoader);
				XposedBridge.hookAllConstructors(hookClass, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
						super.beforeHookedMethod(param);
						int windowType = (int) param.args[4];
						if (windowType < WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW || windowType == WindowManager.LayoutParams.TYPE_WALLPAPER) {
							return;
						}
						int flags = (int) param.args[2];
						flags |= 1 << 6;
						param.args[2] = flags;
					}
				});
			}
		}
	}
}
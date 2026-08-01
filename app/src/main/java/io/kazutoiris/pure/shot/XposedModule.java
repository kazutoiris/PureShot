package io.kazutoiris.pure.shot;

import android.util.SparseIntArray;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Locale;
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
				Class<?> hookClass = XposedHelpers.findClassIfExists(android.view.SurfaceControl.class.getCanonicalName(), classLoader);
				for (Constructor<?> constructor : hookClass.getDeclaredConstructors()) {
					// private SurfaceControl(SurfaceSession session, String name, int w, int h, int format, int flags,
					//         SurfaceControl parent, SparseIntArray metadata, WeakReference<View> localOwnerView,
					//         String callsite)
					XposedBridge.log(String.format(Locale.getDefault(), "current constructor(%d): %s", constructor.getParameterCount(), constructor.toString()));
					if (constructor.getParameterCount() == 10) {
						XposedBridge.log("find hook point!");
						XposedBridge.hookMethod(constructor, new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								super.beforeHookedMethod(param);
								XposedBridge.log("args: " + Arrays.toString(param.args));
								SparseIntArray metadata = (SparseIntArray) param.args[7];
								XposedBridge.log("metadata: " + metadata.toString());
								int windowType = metadata.get(2);
								XposedBridge.log("windowType: " + windowType);
								if (windowType < WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW || windowType == WindowManager.LayoutParams.TYPE_WALLPAPER) {
									XposedBridge.log("ignored current window");
									return;
								}
								int flags = (int) param.args[5];
								flags |= 1 << 6;
								param.args[5] = flags;
							}
						});
					}
				}
			}
		}
	}
}
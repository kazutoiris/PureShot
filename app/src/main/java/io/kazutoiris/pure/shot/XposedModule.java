package io.kazutoiris.pure.shot;

import android.util.SparseIntArray;
import android.view.WindowManager;

import java.lang.reflect.Constructor;
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
					if (constructor.getParameterCount() == 10) {
						XposedBridge.hookMethod(constructor, new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								super.beforeHookedMethod(param);
								String callsite = (String) param.args[9];
								if (!Objects.equals(callsite, "WindowSurfaceController")) {
									return;
								}
								SparseIntArray metadata = (SparseIntArray) param.args[7];
								int windowType = metadata.get(2, -1);
								if (windowType < WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW || windowType == WindowManager.LayoutParams.TYPE_WALLPAPER) {
									return;
								}
								if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S) {
									metadata.put(2, 441731);
								} else {
									int flags = (int) param.args[5];
									flags |= 1 << 6;
									param.args[5] = flags;
								}
							}
						});
					}
				}
			}
			{
				// Android 17+ (flag com.android.window.flags.use_client_surface): the client creates
				// its own buffer surface, so WindowStateAnimator.createSurfaceLocked() returns early and
				// the "WindowSurfaceController" callsite above never fires. Mark the server-owned
				// WindowState container surface instead; SKIP_SCREENSHOT is inherited by its children.
				Class<?> windowStateClass = XposedHelpers.findClassIfExists("com.android.server.wm.WindowState", classLoader);
				if (windowStateClass != null) {
					XposedHelpers.findAndHookMethod(windowStateClass, "setInitialSurfaceControlProperties",
							android.view.SurfaceControl.Builder.class, new XC_MethodHook() {
								@Override
								protected void afterHookedMethod(MethodHookParam param) throws Throwable {
									super.afterHookedMethod(param);
									WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) XposedHelpers.getObjectField(param.thisObject, "mAttrs");
									int windowType = attrs.type;
									if (windowType < WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW || windowType == WindowManager.LayoutParams.TYPE_WALLPAPER) {
										return;
									}
									android.view.SurfaceControl surfaceControl = (android.view.SurfaceControl) XposedHelpers.getObjectField(param.thisObject, "mSurfaceControl");
									if (surfaceControl == null) {
										return;
									}
									Object t = XposedHelpers.callMethod(param.thisObject, "getPendingTransaction");
									XposedHelpers.callMethod(t, "setSkipScreenshot", surfaceControl, true);
								}
							});
				}
			}
		}
	}
}
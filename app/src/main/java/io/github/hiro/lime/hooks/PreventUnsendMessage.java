package io.github.hiro.lime.hooks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class PreventUnsendMessage implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.preventUnsendMessage.checked) return;

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!"sync".equals(param.args[0].toString())) return;

                        try {
                            Object wrapper = param.args[1].getClass().getDeclaredField("a").get(param.args[1]);
                            Field operationResponseField = wrapper.getClass().getSuperclass().getDeclaredField("value_");
                            operationResponseField.setAccessible(true);
                            Object operationResponse = operationResponseField.get(wrapper);
                            if (operationResponse == null) return;

                            ArrayList<?> operations = (ArrayList<?>) operationResponse.getClass().getDeclaredField("a").get(operationResponse);
                            if (operations == null) return;

                            for (Object operation : operations) {
                                Field typeField = operation.getClass().getDeclaredField("c");
                                typeField.setAccessible(true);
                                Object type = typeField.get(operation);

                                if ("NOTIFIED_DESTROY_MESSAGE".equals(type.toString())) {
                                    typeField.set(operation, type.getClass().getMethod("valueOf", String.class).invoke(operation, "DUMMY"));
                                } else if ("RECEIVE_MESSAGE".equals(type.toString())) {
                                    Object message = operation.getClass().getDeclaredField("j").get(operation);
                                    if (message == null) continue;
                                    Map<String, String> contentMetadata = (Map<String, String>) message.getClass().getDeclaredField("k").get(message);
                                    if (contentMetadata != null) {
                                        contentMetadata.remove("UNSENT");
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ("fetchMyEvents".equals(param.args[0].toString())) {
                            XposedBridge.log("param.args[0]: " + param.args[0].toString());
                            XposedBridge.log("param.args[1]: " + param.args[1].toString());

                            // wrapper オブジェクトを取得
                            Object wrapper = param.args[1].getClass().getDeclaredField("a").get(param.args[1]);
                            XposedBridge.log("Wrapper class: " + wrapper.getClass().getName());

                            // Field 'b' を取得
                            Field fieldB = wrapper.getClass().getDeclaredField("b");
                            fieldB.setAccessible(true);
                            Object fieldBValue = fieldB.get(wrapper);

                            // Field 'b' がリスト型であることを確認
                            if (fieldBValue instanceof List) {
                                List<?> squareEvents = (List<?>) fieldBValue;
                                for (Object squareEvent : squareEvents) {
                                    // SquareEvent の Field 'c' を取得 (SquareEventPayload)
                                    Field fieldC = squareEvent.getClass().getDeclaredField("c");
                                    fieldC.setAccessible(true);
                                    Object squareEventPayload = fieldC.get(squareEvent);

                                    // SquareEventPayload の Field 'V4' を取得 (notificationMessage)
                                    Field notificationMessageField = squareEventPayload.getClass().getDeclaredField("V4");
                                    notificationMessageField.setAccessible(true);
                                    Object notificationMessage = notificationMessageField.get(squareEventPayload);

                                    // notificationMessage の Field 'a' を取得 (squareMessage)
                                    Field squareMessageField = notificationMessage.getClass().getDeclaredField("a");
                                    squareMessageField.setAccessible(true);
                                    Object squareMessage = squareMessageField.get(notificationMessage);

                                    // squareMessage のフィールドをログ出力
                                    XposedBridge.log("SquareMessage fields:");
                                    for (Field field : squareMessage.getClass().getDeclaredFields()) {
                                        field.setAccessible(true);
                                        XposedBridge.log("Field: " + field.getName() + " = " + field.get(squareMessage));
                                    }
                                }
                            }
                        }
                    }
                }
        );


    }
}

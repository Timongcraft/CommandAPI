package dev.jorel.commandapi.nms;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import dev.jorel.commandapi.arguments.ExceptionHandlingArgumentType;
import net.minecraft.server.v1_16_R1.ArgumentRegistry;
import net.minecraft.server.v1_16_R1.ArgumentSerializer;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.PacketDataSerializer;

import java.lang.reflect.Method;

public class ExceptionHandlingArgumentSerializer_1_16_R1<T> extends ExceptionHandlingArgumentSerializer_Common<T, PacketDataSerializer> implements ArgumentSerializer<ExceptionHandlingArgumentType<T>> {
	// All the ? here should actually be ArgumentRegistry.a, but that is a private inner class. That makes everything really annoying.
	// TODO: We want to check this reflection, but we can't give ArgumentRegistry.a to the @RequireField annotation
	//  Hopefully something works out, but the preprocessor needs to be expanded first
	private static final NMS.SafeStaticOneParameterMethodHandle<?, ArgumentType> getArgumentTypeInformation;
	private static final NMS.SafeVarHandle<?, MinecraftKey> serializationKey;
	private static final NMS.SafeVarHandle<?, ArgumentSerializer> serializer;

	// Compute all var handles all in one go so we don't do this during main server runtime
	static {
		// We need a reference to the class object for ArgumentRegistry.a,
		// We can get an object from ArgumentRegistry#get(MinecraftKey), then take its class
		Class<?> entryClass = null;
		try {
			Method getInfoByResourceLocation = ArgumentRegistry.class.getDeclaredMethod("a", MinecraftKey.class);
			getInfoByResourceLocation.setAccessible(true);
			Object entryObject = getInfoByResourceLocation.invoke(null, new MinecraftKey("entity"));
			entryClass = entryObject.getClass();
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}

		getArgumentTypeInformation = NMS.SafeStaticOneParameterMethodHandle.ofOrNull(ArgumentRegistry.class, "a", entryClass, ArgumentType.class);
		serializationKey = NMS.SafeVarHandle.ofOrNull(entryClass, "c", MinecraftKey.class);
		serializer = NMS.SafeVarHandle.ofOrNull(entryClass, "b", ArgumentSerializer.class);
	}

	// Serializer_Common methods
	@Override
	protected Object getArgumentTypeInformation(ArgumentType<?> argumentType) {
		return getArgumentTypeInformation.invokeOrNull(argumentType);
	}

	@Override
	protected String getSerializationKey(Object info) {
		return serializationKey.getUnknownInstanceType(info).toString();
	}

	@Override
	protected void serializeBaseTypeToNetwork(ArgumentType<T> baseType, Object baseInfo, PacketDataSerializer packetWriter) {
		serializer.getUnknownInstanceType(baseInfo).a(baseType, packetWriter);
	}

	@Override
	protected void serializeBaseTypeToJson(ArgumentType<T> baseType, Object baseInfo, JsonObject properties) {
		serializer.getUnknownInstanceType(baseInfo).a(baseType, properties);
	}

	// ArgumentSerializer methods
	@Override
	// serializeToNetwork
	public void a(ExceptionHandlingArgumentType<T> argument, PacketDataSerializer packetDataSerializer) {
		commonSerializeToNetwork(argument, packetDataSerializer);
	}

	@Override
	// serializeToJson
	public void a(ExceptionHandlingArgumentType<T> argument, JsonObject properties) {
		commonSerializeToJson(argument, properties);
	}

	@Override
	// deserializeFromNetwork
	public ExceptionHandlingArgumentType<T> b(PacketDataSerializer packetDataSerializer) {
		// Since this class overrides its ArgumentRegistry key with the baseType's,
		// this class's key should never show up in a packet and this method should never
		// be called to deserialize the ArgumentType info that wasn't put into the packet
		// anyway. Also, the server shouldn't ever deserialize a PacketPlay*Out*Commands
		// either. If this method ever gets called, either you or I are doing something very wrong!
		throw new IllegalStateException("This shouldn't happen! See dev.jorel.commandapi.nms.ExceptionHandlingArgumentSerializer_1_16_R1#b for more information");
		// Including a mini-stacktrace here in case this exception shows up
		// on a client-disconnected screen, which is not very helpful
	}
}
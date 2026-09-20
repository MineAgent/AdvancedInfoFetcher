import com.example.aif.InfoProvider;
import com.example.aif.InfoServer;

/**
 * Standalone harness: starts the real HTTP server with a fake info provider, so the transport
 * layer can be curled without launching Minecraft.
 *
 * <p>Compile only the Minecraft-free classes together with this file:</p>
 * <pre>
 * javac --release 25 -encoding UTF-8 -d /tmp/aif-verify \
 *   src/main/java/com/example/aif/{InfoProvider,InfoServer,Help}.java tools/VerifyServer.java
 * java -cp /tmp/aif-verify VerifyServer
 * </pre>
 */
public final class VerifyServer {

	static final class FakeProvider implements InfoProvider {
		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public String unavailableReason() {
			return "n/a";
		}

		@Override
		public String info() {
			return "玩家：FakePlayer\n"
					+ "维度：minecraft:overworld\n"
					+ "坐标：-219.53 105.0 112.31\n"
					+ "方块：-220 105 112\n"
					+ "方位：north\n"
					+ "yaw：-135.2\n"
					+ "pitch：12.4\n"
					+ "选中：1\n"
					+ "背包：\n"
					+ "minecraft:oak_log 12\n"
					+ "minecraft:stone 64\n"
					+ "副手：\n"
					+ "minecraft:torch 3\n"
					+ "盔甲：\n"
					+ "minecraft:diamond_helmet 1\n";
		}
	}

	public static void main(String[] args) throws Exception {
		InfoServer server = new InfoServer(new FakeProvider());
		server.start();
		System.out.println("READY - http://" + InfoServer.HOST + ":" + InfoServer.PORT);
		Thread.sleep(Long.MAX_VALUE);
	}
}

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
		/** First {@code /msg} returns a transcript, every later one is empty (drain semantics). */
		private boolean messagesDrained;

		/** First {@code /sound} returns a transcript, every later one is empty (drain semantics). */
		private boolean soundsDrained;

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public String unavailableReason() {
			return "n/a";
		}

		@Override
		public String messages() {
			if (messagesDrained) {
				return "";
			}
			messagesDrained = true;
			return "[System] [CHAT] DSH加入了游戏\n"
					+ "[CHAT] [Baritone] Baritone settings file not found, resetting.\n"
					+ "[Not Secure] <DSH> hello\n"
					+ "未知的指令, 请检查拼写\n"
					+ "已将游戏模式设置为 创造模式\n";
		}

		@Override
		public String sounds() {
			if (soundsDrained) {
				return "";
			}
			soundsDrained = true;
			return "minecraft:block.stone.break 1.00 0.80\n"
					+ "minecraft:entity.player.step 0.30 1.10\n"
					+ "minecraft:block.stone.break 1.00 1.05\n";
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
					+ "生命值：20.0\n"
					+ "饱食度：18\n"
					+ "饱和度：5.0\n"
					+ "效果：\n"
					+ "minecraft:haste 2 95\n"
					+ "minecraft:speed 1 无限\n";
		}

		@Override
		public String inventory() {
			return "背包：\n"
					+ "minecraft:oak_log 12\n"
					+ "minecraft:stone 64\n"
					+ "副手：\n"
					+ "minecraft:torch 3\n"
					+ "盔甲：\n"
					+ "minecraft:diamond_helmet 1\n"
					+ "熔炉：\n"
					+ "类型：minecraft:furnace\n"
					+ "原料：\n"
					+ "minecraft:raw_iron 8\n"
					+ "燃料：\n"
					+ "minecraft:coal 4\n"
					+ "产物：\n"
					+ "空\n"
					+ "燃烧：0.85\n"
					+ "烧炼：0.45\n"
					+ "箱子：\n"
					+ "类型：minecraft:generic_9x6\n"
					+ "容量：54\n"
					+ "1：\n"
					+ "minecraft:stone 64\n"
					+ "40：\n"
					+ "minecraft:diamond 3\n";
		}
	}

	public static void main(String[] args) throws Exception {
		InfoServer server = new InfoServer(new FakeProvider());
		server.start();
		System.out.println("READY - http://" + InfoServer.HOST + ":" + InfoServer.PORT);
		Thread.sleep(Long.MAX_VALUE);
	}
}

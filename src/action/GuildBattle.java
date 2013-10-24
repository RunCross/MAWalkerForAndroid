package action;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GuildBattle {
	// public static final Action Name = Action.GUILD_BATTLE;

	private static final String URL_GUILD_BATTLE = "http://web.million-arthurs.com/connect/app/fairy/guild_fairy_battle?cyt=1";
	private static byte[] response;

	public enum GuildFairyBattleResult {
		win, lose, escape, unknown
	};

	public static GuildFairyBattleResult FairyBattleResult = GuildFairyBattleResult.unknown;

	public static boolean run() throws Exception {
		FairyBattleResult = GuildFairyBattleResult.unknown;
		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("guild_id", Process.info.gfairy.GuildId));
		post.add(new BasicNameValuePair("no", Process.info.gfairy.No));
		post.add(new BasicNameValuePair("serial_id",
				Process.info.gfairy.SerialId));
		post.add(new BasicNameValuePair("spp_skill_serial",
				Process.info.gfairy.Spp));
		try {
			response = Process.network.ConnectToServer(URL_GUILD_BATTLE, post,
					false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getLocalizedMessage();
			throw ex;
		}

		// Thread.sleep(Process.getRandom(1000, 2000));

		if (Info.Debug) {
			String clazzName = new Object() {
				public String getClassName() {
					String clazzName = this.getClass().getName();
					return clazzName.substring(0, clazzName.lastIndexOf('$'));
				}
			}.getClassName();
			walker.Go.saveXMLFile(response, clazzName);
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			if (GuildDefeat.judge(doc)) {
				Process.AddUrgentTask(Info.EventType.guildTop);
				FairyBattleResult = GuildFairyBattleResult.escape;
				return true;
			}

			ParseUserDataInfo.parse(doc);

			if (xpath.evaluate("//own_fairy_battle_result/winner", doc).equals(
					"1"))
				FairyBattleResult = GuildFairyBattleResult.win;
			else
				FairyBattleResult = GuildFairyBattleResult.lose;

			Process.info.week = xpath
					.evaluate("//week_total_contribution", doc);

			// Process.info.SetTimeoutByAction(Name);

		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;

	}

}

package poc.bijstand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/kinderopvangtoeslag")
public class Kinderopvangtoeslag {

	private static Map<String, Object> results = new HashMap<String, Object>();

	protected static VariableMap prepareVariableMap(JSONObject jsObject) {

		int aantalkinderen = jsObject.getInt("aantalkinderen");
		int opvanguren_werkelijk = jsObject.getInt("opvanguren_werkelijk");
		boolean gastouder = jsObject.getBoolean("gastouder");
		boolean dagopvang = jsObject.getBoolean("dagopvang");
		double inkomen = jsObject.getDouble("inkomen");
		double werkuren = jsObject.getDouble("werkuren");
		double uurprijs = jsObject.getDouble("uurprijs");

		// prepare variables for decision evaluation
		VariableMap variables = Variables.putValue("aantalkinderen", aantalkinderen).putValue("gastouder", gastouder)
				.putValue("dagopvang", dagopvang).putValue("werkuren", werkuren)
				.putValue("opvanguren_werkelijk", opvanguren_werkelijk).putValue("uurprijs", uurprijs)
				.putValue("inkomen", inkomen);
		return variables;
	}

	protected static void parseAndEvaluateDecision(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get the keys of all containing decisions
		Set<String> decisionKeys = drg.getDecisionKeys();
		System.out.println(decisionKeys.toString());

		// get a containing decision by key
		DmnDecision decision = drg.getDecision("kinderopvangtoeslag");
		// get a containing decision by key
		DmnDecision decision1 = drg.getDecision("toeslagKind1");
		DmnDecision decision2 = drg.getDecision("toeslagKind2ev");
		DmnDecision decision3 = drg.getDecision("parameters");
		DmnDecision decision4 = drg.getDecision("opvangkosten");
		DmnDecision decision5 = drg.getDecision("opvanguren");
		DmnDecision decision6 = drg.getDecision("rekenuurprijs");
		DmnDecision decision7 = drg.getDecision("maxuurprijs");
		DmnDecision decision8 = drg.getDecision("maxopvanguren");

		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result3 = dmnEngine.evaluateDecision(decision3, variables);
		DmnDecisionResult result4 = dmnEngine.evaluateDecision(decision4, variables);
		DmnDecisionResult result5 = dmnEngine.evaluateDecision(decision5, variables);
		DmnDecisionResult result6 = dmnEngine.evaluateDecision(decision6, variables);
		DmnDecisionResult result7 = dmnEngine.evaluateDecision(decision7, variables);
		DmnDecisionResult result8 = dmnEngine.evaluateDecision(decision8, variables);
		
		System.out.println( result3.collectEntries("kind1").toArray()[0]);
		
		JSONObject js2 = new JSONObject();
		js2.put("kind1", result3.collectEntries("kind1").toArray()[0]);
		js2.put("kind2ev", result3.collectEntries("kind2ev").toArray()[0]);

		results.put("kinderopvangtoeslag", result.collectEntries("kinderopvangtoeslag").toArray()[0]);
		results.put("toeslagKind1", result1.collectEntries("toeslagKind1").toArray()[0]);
		results.put("toeslagKind2ev", result2.collectEntries("toeslagKind2ev").toArray()[0]);
		results.put("parameters", js2);
		results.put("opvangkosten", result4.collectEntries("opvangkosten").toArray()[0]);
		results.put("opvanguren", result5.collectEntries("opvanguren").toArray()[0]);
		results.put("rekenuurprijs", result6.collectEntries("rekenuurprijs").toArray()[0]);
		results.put("maxuurprijs", result7.collectEntries("maxuurprijs").toArray()[0]);
		results.put("maxopvanguren", result8.collectEntries("maxopvanguren").toArray()[0]);
	}

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response getJSON(InputStream incomingData) {
		StringBuilder crunchifyBuilder = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
			String line = null;
			while ((line = in.readLine()) != null) {
				crunchifyBuilder.append(line);
			}
		} catch (Exception e) {
			System.out.println("Error Parsing: - ");
		}
		JSONObject jsObject = (JSONObject) new JSONTokener(crunchifyBuilder.toString()).nextValue();

		VariableMap variables = prepareVariableMap(jsObject);
		// parse decision from resource input stream
		InputStream inputStream = Kinderopvangtoeslag.class.getResourceAsStream("kinderopvangtoeslag.dmn");

		try {
			parseAndEvaluateDecision(variables, inputStream);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}

		GsonBuilder gsonMapBuilder = new GsonBuilder();
		Gson gsonObject = gsonMapBuilder.create();

		String result = gsonObject.toJson(results);
		return Response.status(200).entity(result).build();
	}
}

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

@Path("/netto-bruto")
public class NettoBruto {

	private static Map<String, Object> results = new HashMap<String, Object>();

	protected static VariableMap prepareVariableMap(JSONObject jsObject) {

		int loonheffingeldersbenut = jsObject.getInt("loonheffingeldersbenut");
		int nettobijstand = jsObject.getInt("nettobijstand");
		int alimentatie = jsObject.getInt("alimentatie");
		int betaal1 = jsObject.getInt("betaal1");
		int dagenmaand1 = jsObject.getInt("dagenmaand1");
		int betaal2 = jsObject.getInt("betaal2");
		int dagenmaand2 = jsObject.getInt("dagenmaand2");

		int maxpremieloon = jsObject.getInt("maxpremieloon");
		int maandenbijstand = jsObject.getInt("maandenbijstand");

		VariableMap variables = Variables.putValue("loonheffingeldersbenut", loonheffingeldersbenut)
				.putValue("nettobijstand", nettobijstand).putValue("alimentatie", alimentatie)
				.putValue("betaal1", betaal1).putValue("dagenmaand1", dagenmaand1).putValue("betaal2", betaal2)
				.putValue("dagenmaand2", dagenmaand2).putValue("maxpremieloon", maxpremieloon)
				.putValue("maandenbijstand", maandenbijstand);

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
		DmnDecision decision = drg.getDecision("premieloonzvw");
		DmnDecision decision1 = drg.getDecision("inkomensafhankelijkebijdrage");
		DmnDecision decision2 = drg.getDecision("loonheffing");

		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);

		results.put("premieloonzvw", result.collectEntries("premieloonzvw").toArray()[0]);
		results.put("loonheffing", result2.collectEntries("loonheffing").toArray()[0]);
		results.put("inkomensafhankelijkebijdrage", result1.collectEntries("inkomensafhankelijkebijdrage").toArray()[0]);

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
		InputStream inputStream = NettoBruto.class.getResourceAsStream("netto-bruto.dmn");

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

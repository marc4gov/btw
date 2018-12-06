package poc.bijstand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionRequirementsGraph;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

 
@Path("/kindgebondenbudget")
public class Kindgebondenbudget {

	private static Map<String, Object> results = new HashMap<String, Object>();

	protected static VariableMap prepareVariableMap(JSONObject jsObject) {

		
		JSONArray kind =jsObject.getJSONArray("kinderen");
		int[] kinderen = new int[kind.length()];
        for (int i = 0; i < kind.length(); i++) {
        	int leeftijd = kind.getJSONObject(i).getInt("leeftijd");
            kinderen[i] = leeftijd;
        }
		boolean alleen = jsObject.getBoolean("alleen");
		double inkomen = jsObject.getDouble("inkomen");

		// prepare variables for decision evaluation
		VariableMap variables = Variables.putValue("kinderen", kinderen).putValue("alleen", alleen).putValue("inkomen",
				inkomen);
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
		DmnDecision decision = drg.getDecision("kindgebondenbudget");
		// get a containing decision by key
		DmnDecision decision1 = drg.getDecision("maxtoeslag");
		DmnDecision decision2 = drg.getDecision("kinderen4plus");
		DmnDecision decision3 = drg.getDecision("toeslag");
		DmnDecision decision4 = drg.getDecision("verhoogmaxbedrag");
		DmnDecision decision5 = drg.getDecision("vermindermaxbedrag");

		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result3 = dmnEngine.evaluateDecision(decision3, variables);
		DmnDecisionResult result4 = dmnEngine.evaluateDecision(decision4, variables);
		DmnDecisionResult result5 = dmnEngine.evaluateDecision(decision5, variables);

		results.put("kindgebondenbudget", result.collectEntries("kindgebondenbudget").toArray()[0]);
		results.put("maxtoeslag", result1.collectEntries("maxtoeslag").toArray()[0]);
		results.put("kinderen4plus", result2.collectEntries("kinderen4plus").toArray()[0]);
		results.put("toeslag", result3.collectEntries("toeslag").toArray()[0]);
		results.put("verhoogmaxbedrag", result4.collectEntries("verhoogmaxbedrag").toArray()[0]);
		results.put("vermindermaxbedrag", result5.collectEntries("vermindermaxbedrag").toArray()[0]);

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
			InputStream inputStream = Kindgebondenbudget.class.getResourceAsStream("kindgebondenbudget.dmn");

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

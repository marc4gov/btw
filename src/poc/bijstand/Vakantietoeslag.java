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

@Path("/vakantietoeslag")
public class Vakantietoeslag {

	private  Map<String, Object> results = new HashMap<String, Object>();

	public  Map<String, Object> getResults() {
		return results;
	}

	protected VariableMap prepareVariableMap(JSONObject jsObject) {

		double nettodeelwelVT = jsObject.getDouble("nettodeelwelVT");

		VariableMap variables = Variables.putValue("nettodeelwelVT", nettodeelwelVT);

		return variables;
	}

	protected void parseAndEvaluateDecision(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get a containing decision by key
		DmnDecision decision = drg.getDecision("VTtabel");
		
		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);

		results.put("VTpercentage", result.collectEntries("VTpercentage").toArray()[0]);
		results.put("correctiefactor", result.collectEntries("correctiefactor").toArray()[0]);
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
		InputStream inputStream = Vakantietoeslag.class.getResourceAsStream("vakantietoeslag.dmn");

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

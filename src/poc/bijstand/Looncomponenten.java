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

@Path("/looncomponenten")
public class Looncomponenten {

	private static Map<String, Object> results = new HashMap<String, Object>();
	public  Map<String, Object> getResults() {
		return results;
	}

	private static Loonheffing loonheffing = new Loonheffing();
	

	protected VariableMap prepareVariableMap(JSONObject jsObject) {

		double brutoloon = jsObject.getDouble("brutoloon");
		double brutocomponenten = jsObject.getDouble("brutocomponenten");
		double WNpremie = jsObject.getDouble("WNpremie");
		double loonheffing = getLoonheffing(brutoloon,brutocomponenten,WNpremie);
		double loonheffingdeelgeenVT = loonheffing
										- getLoonheffingVT(brutoloon,WNpremie);
		
		double werknemerspremie = jsObject.getDouble("werknemerspremie");
		double solidariteitsbijdrage = jsObject.getDouble("solidariteitsbijdrage");
		
		double nettotoekenningen = jsObject.getDouble("nettotoekenningen");
		double nettoinhoudingen = jsObject.getDouble("nettoinhoudingen");
		double VTbijstand = jsObject.getDouble("VTbijstand");
		double bijstandnorm = jsObject.getDouble("bijstandnorm");
				
				
		VariableMap variables = Variables.putValue("brutoloon", brutoloon)
								.putValue("brutocomponenten", brutocomponenten)
								.putValue("WNpremie", WNpremie)
								.putValue("loonheffing", loonheffing)
								.putValue("loonheffingdeelgeenVT", loonheffingdeelgeenVT)
								.putValue("werknemerspremie", werknemerspremie)
								.putValue("solidariteitsbijdrage", solidariteitsbijdrage)
								.putValue("nettotoekenningen", nettotoekenningen)
								.putValue("nettoinhoudingen", nettoinhoudingen)
								.putValue("VTbijstand", VTbijstand)
								.putValue("bijstandnorm", bijstandnorm);
								
		
		return variables;
	}	
	
	
	protected void parseAndEvaluateDecision(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get the keys of all containing decisions
		Set<String> decisionKeys = drg.getDecisionKeys();
		System.out.println(decisionKeys.toString());

		// get a containing decision by key
		DmnDecision decision = drg.getDecision("netto");
		DmnDecision decision1 = drg.getDecision("nettodeelgeenVT");
		DmnDecision decision2 = drg.getDecision("nettobijstand");
		DmnDecision decision5 = drg.getDecision("heffingsloon");
		
		
		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result5 = dmnEngine.evaluateDecision(decision5, variables);
		

		results.put("netto", result.collectEntries("netto").toArray()[0]);
		results.put("nettodeelgeenVT", result1.collectEntries("nettodeelgeenVT").toArray()[0]);
		results.put("nettobijstand", result2.collectEntries("nettobijstand").toArray()[0]);
		results.put("heffingsloon", result5.collectEntries("heffingsloon").toArray()[0]);
		
	}

	static Map<String, Object> getResultsFromLoonheffing(double brutoloon, double brutocomponenten, double WNpremie) {
		JSONObject jsObject = new JSONObject();
		jsObject.put("brutoloon", brutoloon).put("brutocomponenten", brutocomponenten).put("WNpremie", WNpremie);
		VariableMap variables = loonheffing.prepareVariableMap(jsObject);
		InputStream inputStream = Loonheffing.class.getResourceAsStream("loonheffing.dmn");

		try {
			loonheffing.parseAndEvaluateDecision(variables, inputStream);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
		return loonheffing.getResults();
	}
	
	private static double getLoonheffingVT(double brutoloon, double WNpremie) {
		return (double) getResultsFromLoonheffing(brutoloon, 0, WNpremie).get("loonheffing");
	}
	
	private static double getLoonheffing(double brutoloon, double brutocomponenten, double WNpremie) {
		return (double) getResultsFromLoonheffing(brutoloon, brutocomponenten, WNpremie).get("loonheffing");
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
		InputStream inputStream = Looncomponenten.class.getResourceAsStream("looncomponenten.dmn");

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

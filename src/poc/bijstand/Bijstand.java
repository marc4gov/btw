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

@Path("/bijstand")
public class Bijstand {

	private static Map<String, Object> results = new HashMap<String, Object>();

	public static  Map<String, Object> getResults() {
		return results;
	}
	
	private static Looncomponenten looncomponenten = new Looncomponenten();
	
	protected static VariableMap prepareVariableMap(JSONObject jsObject) {
		
		double nettobijstand = 0.0;
		if (jsObject.has("nettobijstand")) {
			nettobijstand = jsObject.getDouble("nettobijstand");			
		} 
		double nettodeelgeenVT = 0.0;
		if (jsObject.has("nettodeelgeenVT")) {
			nettodeelgeenVT = jsObject.getDouble("nettodeelgeenVT");
		}
		double vakantiegeldwerkgever = 0.0;
		if (jsObject.has("nettobijstand") && jsObject.has("nettodeelgeenVT") ) {
			 vakantiegeldwerkgever = (double) getResultsFromVakantietoeslagTabel(nettobijstand,nettodeelgeenVT).get("vakantiegeldwerkgever");
		}
		
		double VTbijstand = jsObject.getDouble("VTbijstand");
		double bijstandnorm = jsObject.getDouble("bijstandnorm");
				
		VariableMap variables = Variables
								.putValue("nettodeelgeenVT", nettodeelgeenVT)
								.putValue("nettobijstand", nettobijstand)
								.putValue("VTbijstand", VTbijstand)
								.putValue("vakantiegeldwerkgever", vakantiegeldwerkgever)
								.putValue("bijstandnorm", bijstandnorm);
		return variables;
	}	
		

	static Map<String, Object> getResultsFromVakantietoeslagTabel(double nettobijstand, double nettodeelgeenVT) {
		VariableMap variables = Variables
				.putValue("nettodeelgeenVT", nettodeelgeenVT)
				.putValue("nettobijstand", nettobijstand);
		InputStream inputStream = Bijstand.class.getResourceAsStream("bijstand.dmn");

		try {
			parseAndEvaluateDecisionVT(variables, inputStream);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
		return getResults();
	}
	
	static Map<String, Object> getResultsFromLooncomponenten(JSONObject jsObject) {
		VariableMap variables = looncomponenten.prepareVariableMap(jsObject);
		System.out.println(jsObject.toString());
		InputStream inputStream = Looncomponenten.class.getResourceAsStream("looncomponenten.dmn");

		try {
			looncomponenten.parseAndEvaluateDecision(variables, inputStream);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
		return looncomponenten.getResults();
	}

	
	
	protected static void parseAndEvaluateDecisionVT(VariableMap variables, InputStream inputStream) {

		// create a new default DMN engine
		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		// parse the drg from an input stream
		DmnDecisionRequirementsGraph drg = dmnEngine.parseDecisionRequirementsGraph(inputStream);

		// get a containing decision by key
		DmnDecision decision = drg.getDecision("VTtabel");
		DmnDecision decision1 = drg.getDecision("nettodeelwelVT");
		
		// evaluate decision
		DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		
		double nettodeelwelVT = (double) result1.collectEntries("nettodeelwelVT").toArray()[0];
		double VTpercentage = (double) result.collectEntries("VTpercentage").toArray()[0];
		double correctiefactor = (double) result.collectEntries("correctiefactor").toArray()[0];
		results.put("vakantiegeldwerkgever", VTpercentage*nettodeelwelVT + correctiefactor);
		
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
		DmnDecision decision1 = drg.getDecision("tekorten");
		DmnDecision decision2 = drg.getDecision("aanvullendebijstand");
		DmnDecision decision3 = drg.getDecision("nettodeelwelVT");
		DmnDecision decision4 = drg.getDecision("inkomstenVT");
		
		// evaluate decision
		DmnDecisionResult result1 = dmnEngine.evaluateDecision(decision1, variables);
		DmnDecisionResult result2 = dmnEngine.evaluateDecision(decision2, variables);
		DmnDecisionResult result3 = dmnEngine.evaluateDecision(decision3, variables);
		DmnDecisionResult result4 = dmnEngine.evaluateDecision(decision4, variables);

		results.put("tekorten", result1.collectEntries("tekorten").toArray()[0]);
		results.put("aanvullendebijstand", result2.collectEntries("aanvullendebijstand").toArray()[0]);
		results.put("nettodeelwelVT", result3.collectEntries("nettodeelwelVT").toArray()[0]);
		results.put("inkomstenVT", result4.collectEntries("inkomstenVT").toArray()[0]);
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
		
		Map<String, Object> res = getResultsFromLooncomponenten(jsObject);
		jsObject.put("nettobijstand", (double) res.get("nettobijstand"));
		jsObject.put("nettodeelgeenVT", (double) res.get("nettodeelgeenVT"));
		
		getResultsFromVakantietoeslagTabel((double)jsObject.get("nettobijstand"), (double)jsObject.get("nettodeelgeenVT"));
		
		jsObject.put("vakantiegeldwerkgever", (double)results.get("vakantiegeldwerkgever"));
		VariableMap variables = prepareVariableMap(jsObject);
		InputStream inputStream = Bijstand.class.getResourceAsStream("bijstand.dmn");

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

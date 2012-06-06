package examples;

import java.util.ArrayList;
import javax.management.Attribute;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.redhat.qe.katello.base.KatelloApi;
import com.redhat.qe.katello.base.KatelloPostParam;
import com.redhat.qe.katello.base.KatelloTestScript;
import com.redhat.qe.katello.base.obj.KatelloEnvironment;
import com.redhat.qe.katello.base.obj.KatelloOrg;
import com.redhat.qe.tools.SSHCommandResult;

public class DemoKatelloApi {

	@Test(description="demo KatelloApi.get")
	public void test_apiGet(){
		new KatelloApi().get(KatelloOrg.API_CMD_LIST);
	}
	
	@Test(description="demo KatelloApi.post - simple")
	public void test_apiPost_simple(){
		ArrayList<Attribute> opts = new ArrayList<Attribute>(); 
		String rand = KatelloTestScript.getUniqueID();
		opts.add(new Attribute("name", "demoApi-"+rand));
		opts.add(new Attribute("description", "simple description - here it can be null as well"));
		KatelloApi api = new KatelloApi();
		KatelloPostParam[] params = {new KatelloPostParam(null, opts)};
		api.post(params,KatelloOrg.API_CMD_CREATE); // "SSHCommandResult res = " could be used to extract/assert details from the result
	}

	@Test(description="demo KatelloApi.post - more complex")
	public void test_apiPost_complex(){
		KatelloApi api = new KatelloApi();
		ArrayList<Attribute> opts = new ArrayList<Attribute>(); 
		String rand = KatelloTestScript.getUniqueID();

		opts.add(new Attribute("name", "demoApi-env-"+rand));
		opts.add(new Attribute("description", null));
		opts.add(new Attribute("prior", "1")); // otherwise you need to use KatelloEnvironment.get_prior_id() for that KatelloEnvironment.LIBRARY env.
		KatelloPostParam[] params = {new KatelloPostParam("environment", opts)};
		api.post(params, String.format(KatelloEnvironment.API_CMD_CREATE, "ACME_Corporation"));
	}

}
package com.redhat.qe.katello.base;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;
import org.testng.Assert;
import com.redhat.qe.katello.base.cli.KatelloProvider;
import com.redhat.qe.katello.base.cli.KatelloRepo;
import com.redhat.qe.katello.common.KatelloConstants;
import com.redhat.qe.katello.tasks.KatelloTasks;
import com.redhat.qe.tools.ExecCommands;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

public class KatelloCliTestScript 
extends com.redhat.qe.auto.testng.TestScript 
implements KatelloConstants {

	protected static Logger log = Logger.getLogger(KatelloCliTestScript.class.getName());

	protected KatelloTasks servertasks	= null;
	protected KatelloTasks clienttasks = null;
	
	private int platform_id = -1; // made a class property - in case in the tests there would be a need to check platform.
	public KatelloCliTestScript() {
		super();
		try {
			SSHCommandRunner server_sshRunner = null;
			SSHCommandRunner client_sshRunner = null;
			try{
				server_sshRunner = new SSHCommandRunner(
						System.getProperty("katello.server.hostname", "localhost"), 
						System.getProperty("katello.ssh.user", "root"), 
						System.getProperty("katello.ssh.passphrase", "secret"), 
						System.getProperty("katello.sshkey.private", ".ssh/id_hudson_dsa"), 
						System.getProperty("katello.sshkey.passphrase", "secret"), null);				
			}catch(Throwable t){
				log.warning("Warning: Could not initialize server's SSHCommandRunner.");
				t.printStackTrace();
			}
			try{
				client_sshRunner = new SSHCommandRunner(
						System.getProperty("katello.client.hostname", "localhost"), 
						System.getProperty("katello.client.ssh.user", "root"), 
						System.getProperty("katello.client.ssh.passphrase", "secret"), 
						System.getProperty("katello.client.sshkey.private", ".ssh/id_hudson_dsa"), 
						System.getProperty("katello.client.sshkey.passphrase", "secret"), null);				
			}catch(Throwable t){
				log.warning("Warning: Could not initialize client's SSHCommandRunner.");
				t.printStackTrace();
			}

			ExecCommands localRunner = new ExecCommands();
			servertasks = new KatelloTasks(server_sshRunner, localRunner);
			clienttasks = new KatelloTasks(client_sshRunner, localRunner);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public int getClientPlatformID(){
		return this.platform_id;
	}
	
	protected void assert_providerRemoved(KatelloProvider prov){
		SSHCommandResult res;
		log.info("Assertions: provider has been removed");
		res = prov.info();
		Assert.assertTrue(res.getExitCode().intValue()==65, "Check - return code");
		Assert.assertEquals(getOutput(res).trim(), "Could not find provider [ "+prov.name+" ] " +
				"within organization [ "+prov.org+" ]", "Check - `provider info` return string");
	}
	
	protected void assert_repoSynced(KatelloRepo repo){
		SSHCommandResult res;
		String REGEXP_REPO_INFO;
		log.info("Assertions: repository has been synchronized");
		res = repo.info();
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		// pacakge_count != 0
		REGEXP_REPO_INFO = ".*Name:\\s+"+repo.name+".*Package Count:\\s+0.*";
		Assert.assertFalse(getOutput(res).replaceAll("\n", "").matches(REGEXP_REPO_INFO), 
				"Repo should not contain packages count: 0");
		// last_sync != never
		REGEXP_REPO_INFO = ".*Name:\\s+"+repo.name+".*Last Sync:\\s+never.*";
		Assert.assertFalse(getOutput(res).replaceAll("\n", "").matches(REGEXP_REPO_INFO), 
				"Repo should not contain last_sync == never");
		// progress != Not synced
		REGEXP_REPO_INFO = ".*Name:\\s+"+repo.name+".*Progress:\\s+Not synced.*";
		Assert.assertFalse(getOutput(res).replaceAll("\n", "").matches(REGEXP_REPO_INFO), 
				"Repo should not contain progress == not synced");
		
		// package_count >0; url, progress, last_sync
		String cnt = KatelloTasks.grepCLIOutput("Package Count", res.getStdout());
		Assert.assertTrue(new Integer(cnt).intValue()>0, "Repo should contain packages count: >0");
		REGEXP_REPO_INFO = ".*Name:\\s+"+repo.name+".*Progress:\\s+Finished.*";
		Assert.assertTrue(getOutput(res).replaceAll("\n", "").matches(REGEXP_REPO_INFO), 
				"Repo should contain progress == finished");
	}
	
	protected void waitfor_reposync(KatelloRepo repo, int timeoutMinutes){
		SSHCommandResult res;
		long now = Calendar.getInstance().getTimeInMillis() / 1000;
		long start = now;
		long maxWaitSec = start + (timeoutMinutes * 60);
		String REGEXP_STATUS_FINISHED = ".*Sync State:\\s+Finished.*";
		log.fine("Waiting repo sync finish for: minutes=["+timeoutMinutes+"]; " +
				"org=["+repo.org+"]; product=["+repo.product+"]; repo=["+repo.name+"]");
		while(now<maxWaitSec){
			res = repo.status();
			now = Calendar.getInstance().getTimeInMillis() / 1000;
			if(getOutput(res).replaceAll("\n", "").matches(REGEXP_STATUS_FINISHED))
				break;
			try{Thread.sleep(60000);}catch (Exception e){}
		}
		if(now<=maxWaitSec)
			log.fine("Repo sync done in: ["+String.valueOf((Calendar.getInstance().getTimeInMillis() / 1000) - start)+"] sec");
		else
			log.warning("Repo sync did not finished after: ["+String.valueOf(maxWaitSec - start)+"] sec");
	}
	
	/**
	 * Returns list of org names that have imported a manifest that has subscriptions for:<BR>
	 * Red Hat Enterprise Linux Server<BR>
	 * Id: 69
	 * @return empty list or names of the orgs
	 */
	protected ArrayList<String> getOrgsWithImportedManifest(){
		ArrayList<String> orgs = new ArrayList<String>();
		String servername = System.getProperty("katello.server.hostname", "localhost");
		log.info("Scanning ["+servername+"] for organizations with imported manifest");
		KatelloCli cli = new KatelloCli("org list -v | grep \"^Name\" | cut -d: -f2", null);
		SSHCommandResult res = cli.run();
		String[] lines = getOutput(res).split("\n");
		for(String org: lines){
			org = org.trim();
			res = new KatelloCli("product list --provider=\""+KatelloProvider.PROVIDER_REDHAT+
					"\" --org \""+org+"\" -v | grep \"^Id:\\s\\+69\" | wc -l",null).run();
			if(getOutput(res).equals("1")){
				orgs.add(org);
			}
		}
		return orgs;
	}
	
	protected boolean hasOrg_environment(String org, String environment){
		log.info(String.format("Check if the org [%s] has an environment [%s]",org,environment));
		SSHCommandResult res = new KatelloCli("environment list"+
				"\" --org \""+org+"\" -v | grep \"^Name:\\s\\+"+environment+"\" | wc -l",null).run();
		return getOutput(res).equals("1");
	}
	
	protected SSHCommandResult rhsm_clean(){
		log.info("RHSM clean");
		return clienttasks.execute_remote("subscription-manager clean");
	}
	
	protected SSHCommandResult rhsm_register(String org, String environment, String name, boolean autosubscribe){
		log.info("Registering client with: --org \""+org+"\" --environment \""+environment+"\" " +
				"--name \""+name+"\" --autosubscribe "+Boolean.toString(autosubscribe));
		String cmd = String.format(
				"subscription-manager register --username admin --password admin --org \"%s\" --environment \"%s\" --name \"%s\"",
				org,environment,name);
		if(autosubscribe)
			cmd += " --autosubscribe";
		return clienttasks.execute_remote(cmd);
	}
	
	protected String getOutput(SSHCommandResult res){
		return sgetOutput(res);
	}
	
	public static String sgetOutput(SSHCommandResult res){
		return (res.getStdout()+"\n"+res.getStderr()).trim();
	}
	
}

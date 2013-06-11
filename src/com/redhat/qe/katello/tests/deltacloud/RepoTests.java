package com.redhat.qe.katello.tests.deltacloud;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.obj.KatelloActivationKey;
import com.redhat.qe.katello.base.obj.KatelloChangeset;
import com.redhat.qe.katello.base.obj.KatelloErrata;
import com.redhat.qe.katello.base.obj.KatelloPackage;
import com.redhat.qe.katello.base.obj.KatelloProduct;
import com.redhat.qe.katello.base.obj.KatelloRepo;
import com.redhat.qe.katello.base.obj.KatelloSystem;
import com.redhat.qe.katello.common.KatelloUtils;

@Test(dependsOnGroups="cfse-pack")
public class RepoTests extends BaseDeltacloudTest {
	
	private String ert1;
	private String content_view_promote_package;
	private String content_view_remove_package;
	private String content_view_promote_errata;
	private String content_view_remove_errata;

	private void configureClient(String activationKey, String contentView, String client, String systemName, String envName) {
		rhsm_clean(client);
		
		KatelloActivationKey act_key = new KatelloActivationKey(org_name, envName, activationKey, "Act key created");
		exec_result = act_key.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");      
		exec_result = act_key.update_add_content_view(contentView);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");      
		exec_result = act_key.update_add_subscription(poolId1);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		exec_result = act_key.update_add_subscription(poolId2);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		KatelloSystem sys = new KatelloSystem(systemName, org_name, envName);
		sys.runOn(client);
		exec_result = sys.rhsm_registerForce(activationKey); 
		Assert.assertTrue(exec_result.getExitCode().intValue() == 0, "Check - return code");
		exec_result = sys.rhsm_identity();
		system_uuid3 = KatelloUtils.grepCLIOutput("Current identity is", exec_result.getStdout());
		
		KatelloUtils.sshOnClient(client, "sed -i -e \"s/certFrequency.*/certFrequency = 1/\" /etc/rhsm/rhsm.conf");
		KatelloUtils.sshOnClient(client, "service rhsmcertd restart");
		yum_clean(client);
		KatelloUtils.sshOnClient(client, "service goferd restart;");		
	}
	
	@Test(description="promote rhel packages")
	public void test_promoteRHELPackages() {
		content_view_promote_package = KatelloUtils.promotePackagesToEnvironment(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, new String[] {"zsh"}, env_name2);

		configureClient("actkeypackagepromote" + uid, content_view_promote_package, client_name3, system_name3, env_name2);
	}
	
	@Test(description="list rhel repo packages promoted to test environment", dependsOnMethods={"test_promoteRHELPackages"})
	public void test_listRHELRepoPackages() {
		KatelloPackage pack = new KatelloPackage(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, content_view_promote_package);
		exec_result = pack.cli_search("*zsh*");
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).trim().contains("zsh"), "Package zsh is listed in environment package list");
	}

	@Test(description="delete promoted rhel packages", dependsOnMethods={"test_listRHELRepoPackages"})
	public void test_deleteRHELPackages() {
		content_view_remove_package = KatelloUtils.removePackagesFromEnvironment(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, new String[] {"zsh"}, env_name2);
		
		configureClient("actkeypackageremove" + uid, content_view_remove_package, client_name3, system_name3, env_name2);
	}
	
	//@ TODO enable when bug 918093 is fixed
	@Test(description="list rhel repo packages deleted to test environment", dependsOnMethods={"test_deleteRHELPackages"}, enabled=true)
	public void test_listRHELRepoPackagesDeleted() {
		KatelloPackage pack = new KatelloPackage(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, content_view_remove_package);
		exec_result = pack.cli_search("*zsh*");
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertFalse(getOutput(exec_result).trim().contains("zsh"), "Package zsh is not listed in environment package list");
	}
	
	@Test(description="promote rhel distr", dependsOnMethods={"test_listRHELRepoPackagesDeleted"})
	public void test_promoteRHELDistr() {
		KatelloChangeset cs = new KatelloChangeset("package-promote"+KatelloUtils.getUniqueID(), org_name, env_name2);
		exec_result = cs.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code (changeset create)");
		
		exec_result = cs.update_add_distr(KatelloProduct.RHEL_SERVER, "ks-Red Hat Enterprise Linux-Server-6.4-x86_64");
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code (changeset update add package)");
		
		exec_result = cs.apply();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code (changeset promote)");
		
		rhsm_clean(client_name3);
		
		KatelloSystem sys = new KatelloSystem(system_name3, org_name, env_name2);
		sys.runOn(client_name3);
		exec_result = sys.rhsm_registerForce(); 
		Assert.assertTrue(exec_result.getExitCode().intValue() == 0, "Check - return code");
		exec_result = sys.rhsm_identity();
		system_uuid3 = KatelloUtils.grepCLIOutput("Current identity is", exec_result.getStdout());
	}
	
	@Test(description="install zsh package", dependsOnMethods={"test_promoteRHELDistr"})
	public void test_installRHELPackage() {
		KatelloUtils.sshOnClient(client_name3, "yum clean all");
		exec_result = KatelloUtils.sshOnClient(client_name3, "yum repolist");
		Assert.assertTrue(getOutput(exec_result).trim().contains("Red Hat Enterprise Linux Server"), "Contains RHEL repo");
		KatelloUtils.sshOnClient(client_name3, "yum erase -y zsh");
		exec_result = KatelloUtils.sshOnClient(client_name3, "yum install -y zsh beaker*");
		Assert.assertTrue(exec_result.getExitCode().intValue()==0, "Check - return code (yum install zsh)");
		exec_result = KatelloUtils.sshOnClient(client_name3, "rpm -q zsh");
		Assert.assertTrue(exec_result.getExitCode().intValue()==0, "Check - return code (rpm -q zsh)");
	}
	
	@Test(description="promote rhel errata", dependsOnMethods={"test_installRHELPackage"})
	public void test_promoteRHELErrata() {
		
		KatelloSystem sys = new KatelloSystem(system_name, org_name, null);
		exec_result = sys.list_errata_names("RHBA");
		ert1 = getOutput(exec_result).replaceAll("\n", ",").split(",")[0];
	
		content_view_promote_errata = KatelloUtils.promoteErratasToEnvironment(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, new String[] {ert1}, env_name2);	
		
		configureClient("actkeyerratapromote" + uid, content_view_promote_errata, client_name3, system_name3, env_name2);
	}
	
	@Test(description="list rhel repo errata promoted to test environment", dependsOnMethods={"test_promoteRHELErrata"})
	public void test_listRHELRepoErrata() {
		KatelloErrata errata = new KatelloErrata(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, content_view_promote_errata);
		exec_result = errata.cli_list();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).trim().contains(ert1), "Errata " + ert1 + " is listed in environment errata list");
	}

	@Test(description="delete promoted rhel errata", dependsOnMethods={"test_listRHELRepoErrata"})
	public void test_deleteRHELErrata() {
		content_view_remove_errata = KatelloUtils.removeErratasFromEnvironment(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, new String[] {ert1}, env_name2);
		
		configureClient("actkeyerrataremove" + uid, content_view_remove_errata, client_name3, system_name3, env_name2);
	}
	
	@Test(description="list rhel repo errata deleted to test environment", dependsOnMethods={"test_deleteRHELErrata"})
	public void test_listRHELRepoErrataDeleted() {
		KatelloErrata errata = new KatelloErrata(org_name, KatelloProduct.RHEL_SERVER, KatelloRepo.RH_REPO_RHEL6_SERVER_RPMS_64BIT, content_view_remove_errata);
		exec_result = errata.cli_list();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertFalse(getOutput(exec_result).trim().contains(ert1), "Errata " + ert1 + " is not listed in environment errata list");
	}
}

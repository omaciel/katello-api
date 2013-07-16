package com.redhat.qe.katello.tests.cli;

import java.util.logging.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.KatelloCliTestBase;
import com.redhat.qe.katello.base.obj.KatelloPackageGroup;
import com.redhat.qe.katello.base.obj.KatelloRepo;
import com.redhat.qe.katello.base.obj.KatelloUser;
import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.katello.common.TngRunGroups;
import com.redhat.qe.tools.SSHCommandResult;

@Test(groups={"cfse-cli",TngRunGroups.TNG_KATELLO_Content})
public class PackageGroupTests extends KatelloCliTestBase {

	protected static Logger log = Logger.getLogger(PackageGroupTests.class.getName());
	private String user_name;
	
	@BeforeClass(description="Generate unique objects")
	public void setUp() {
		// Create user:
		user_name = "user"+KatelloUtils.getUniqueID();
		KatelloUser user = new KatelloUser(cli_worker, user_name, "root@localhost", KatelloUser.DEFAULT_USER_PASS, false);
		exec_result = user.cli_create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
	}
	
	@Test(description="packagegroup list", groups = {"cli-packagegroup"}, enabled=true)
	public void test_packageGroupList() {
		KatelloRepo repo = new KatelloRepo(this.cli_worker, base_zoo_repo_name, base_org_name, base_zoo_product_name, REPO_INECAS_ZOO3, null, null);
		exec_result = repo.info();
		String repo_id = KatelloUtils.grepCLIOutput("ID", getOutput(exec_result).trim(),1);
		
		KatelloPackageGroup packGr = new KatelloPackageGroup(cli_worker, null, null, null);
		
		exec_result = packGr.cli_list(repo_id);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		Assert.assertTrue(getOutput(exec_result).contains("mammals"));
		Assert.assertTrue(getOutput(exec_result).contains("birds"));
		
		packGr.id = grep_packageGroupID(getOutput(exec_result), "birds");
		packGr.name = "birds";
		assert_packageGroupInfo(packGr, repo_id);
		
		packGr.id = grep_packageGroupID(getOutput(exec_result), "mammals");
		packGr.name = "mammals";
		assert_packageGroupInfo(packGr, repo_id);
	}
	
	@Test(description="package_group category list")
	public void testPackageGroupCategoryList() {
		KatelloRepo repo = new KatelloRepo(cli_worker, base_zoo_repo_name, base_org_name, base_zoo_product_name, REPO_INECAS_ZOO3, null, null);
		exec_result = repo.info();
		String repo_id = KatelloUtils.grepCLIOutput("ID", getOutput(exec_result).trim(),1);

		KatelloPackageGroup packGr = new KatelloPackageGroup(cli_worker, null, null, null);
		exec_result = packGr.cli_category_list(repo_id);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check exit code (package_group category_list)");
		String category_id = KatelloUtils.grepCLIOutput("ID", getOutput(exec_result).trim(),1);

		exec_result = packGr.cli_category_info(repo_id, category_id);
		Assert.assertTrue(exec_result.getExitCode()==0, "Check exit code (package_group category_info)");
	}

	private void assert_packageGroupInfo(KatelloPackageGroup pack, String repoId){
		SSHCommandResult res;
		res = pack.cli_info(repoId);
		Assert.assertTrue(res.getExitCode() == 0, "Check - return code");
		
		Assert.assertTrue(getOutput(res).contains(pack.id), "Check - package group Id should exist in list result");		
		Assert.assertTrue(getOutput(res).contains(pack.name), "Check - package group name should exist in list result");
	}

	private String grep_packageGroupID(String output, String groupname) {
		String[] lines = output.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].contains(groupname)) {
				return lines[i].split(" ")[0];
			}
		}
		return null;
	}
}

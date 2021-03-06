package com.redhat.qe.katello.tests.cli;

import java.util.logging.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.KatelloCliTestBase;
import com.redhat.qe.katello.base.obj.KatelloErrata;
import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.katello.common.TngRunGroups;
import com.redhat.qe.katello.tests.e2e.PromoteErrata;

@Test(groups={"cfse-cli",TngRunGroups.TNG_KATELLO_Errata})
public class ErrataTests extends KatelloCliTestBase {

	protected static Logger log = Logger.getLogger(ErrataTests.class.getName());
	
	private String content_view;
	
	@BeforeClass(description="Generate unique objects", alwaysRun=true)
	public void setUp() {
		content_view = KatelloUtils.promoteRepoToEnvironment(
				cli_worker, base_org_name, base_zoo_product_name, base_zoo_repo_name, base_test_env_name);
	}
	
	@Test(description="8782a6e0-f41a-48d5-8599-bfe7f24078f6")
	public void test_errataList() {
		KatelloErrata errata = new KatelloErrata(cli_worker, base_org_name, base_zoo_product_name, base_zoo_repo_name, content_view);
		errata.setProductId(base_zoo_product_id);
		
		exec_result = errata.cli_list();
		Assert.assertTrue(exec_result.getExitCode().intValue()==0, "Check - return code (errata list)");
		Assert.assertTrue(getOutput(exec_result).replaceAll("\n", "").contains(PromoteErrata.ERRATA_ZOO_SEA), "Check - errata list output");
	}

	@Test(description="2542ae61-8de6-40ce-866f-999c39fa9018")
	public void test_errataInfo() {
		KatelloErrata errata = new KatelloErrata(cli_worker, base_org_name, base_zoo_product_name, base_zoo_repo_name, content_view);
		errata.setId(PromoteErrata.ERRATA_ZOO_SEA);
		errata.setProductId(base_zoo_product_id);
		
		exec_result = errata.info();
		Assert.assertTrue(exec_result.getExitCode().intValue()==0, "Check - return code (errata info)");
		Assert.assertTrue(getOutput(exec_result).replaceAll("\n", "").contains(PromoteErrata.ERRATA_ZOO_SEA), "Check - errata info output");
	}
}

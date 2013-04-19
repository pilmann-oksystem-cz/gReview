package com.houghtonassociates.bamboo.plugins.processor;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.container.BambooContainer;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.houghtonassociates.bamboo.plugins.GerritRepositoryAdapter;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;

/**
 * Provides business logic after full build result is available.<br>
 * Main task of this class is set appropriate verification flag (in Gerrit's
 * change) prior to build result.
 */
public class GerritPostChainAction implements PostChainAction {

    @Override
    public void
                    execute(Chain chain,
                            ChainResultsSummary chainResultsSummary,
                            ChainExecution chainExecution) throws InterruptedException,
                                    Exception {
        // Obtain repository
        RepositoryDefinition repositoryDefinition = null;
        GerritRepositoryAdapter repository = null;
        for (RepositoryDefinition rd : chain
            .getEffectiveRepositoryDefinitions()) {
            if (rd.getRepository() instanceof GerritRepositoryAdapter) {
                repositoryDefinition = rd;
                repository = (GerritRepositoryAdapter) rd.getRepository();
                break;
            }
        }

        if (repositoryDefinition != null && repository != null) {
            // Obtain GerritService
            GerritService service = repository.getGerritDAO();

            // Obtain Change's revision
            String revision =
                chainExecution.getBuildChanges().getVcsRevisionKey(
                    repositoryDefinition.getId());

            // Obtain Gerrit's change
            GerritChangeVO change = service.getChangeByRevision(revision);

            BuildState status = chainResultsSummary.getBuildState();

            StringBuilder message =
                new StringBuilder(
                    String.format("Build status: %s.\n\n", status));
            message.append(String.format("Build URL: %s/browse/%s",
                BambooContainer.getBambooContainer()
                    .getAdministrationConfiguration().getBaseUrl(),
                chainResultsSummary.getPlanResultKey()));

            service.verifyChange(BuildState.SUCCESS.equals(status),
                change.getNumber(), change.getCurrentPatchSet().getNumber(),
                message.toString());
        }

    }
}

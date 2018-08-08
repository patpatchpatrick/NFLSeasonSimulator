package io.github.patpatchpatrick.nflseasonsim.presenter;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.EloValuesMvpContract;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;

public class EloValuesPresenter extends BasePresenter<EloValuesMvpContract.EloValuesView> implements EloValuesMvpContract.EloValuesPresenter {

    public EloValuesPresenter(EloValuesMvpContract.EloValuesView view) {
        super(view);
    }


}

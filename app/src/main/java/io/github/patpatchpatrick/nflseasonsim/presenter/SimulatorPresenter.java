package io.github.patpatchpatrick.nflseasonsim.presenter;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter {


    SimulatorPresenter(SimulatorMvpContract.SimulatorView view){
        super(view);
    }

    @Override
    public void simulateWeek() {

    }

    @Override
    public void simulateSeason() {

    }
}

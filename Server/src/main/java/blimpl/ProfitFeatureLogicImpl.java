package blimpl;

import beans.InvestStyleAnalyse;
import beans.PriceInfo;
import beans.RiskProfitIndex;
import bl.MarketLogic;
import bl.ProfitFeatureLogic;
import bl.RiskFeatureLogic;
import dataservice.BaseInfoDataService;
import dataserviceimpl.DataServiceController;
import entities.FundInfosEntity;
import exception.ObjectNotFoundException;
import exception.ParameterException;
import util.CalendarOperate;
import util.NumberOpe;
import util.UnitType;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Daniel on 2016/8/15.
 */
public class ProfitFeatureLogicImpl extends UnicastRemoteObject implements ProfitFeatureLogic {

    private static ProfitFeatureLogic instance;
    private MarketLogic marketLogic;
    private BaseInfoDataService baseInfoDataService;

    private ProfitFeatureLogicImpl() throws RemoteException {
        marketLogic = BLController.getMarketLogic();
        baseInfoDataService = DataServiceController.getBaseInfoDataService();
    }

    public static ProfitFeatureLogic getInstance() {
        if (instance == null)
            try {
                instance = new ProfitFeatureLogicImpl();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        return instance;
    }

    @Override
    public double getAlpha(String code) throws RemoteException, ObjectNotFoundException {
        AlphaBetaCal alphaBetaCal = new AlphaBetaCal(code);
        double[] re = alphaBetaCal.getAlphaBeta();
        if (re == null) {
            throw new ObjectNotFoundException("");
        }
        return re[0];
    }

    @Override
    public double aveProfitRate(String code) throws RemoteException, ObjectNotFoundException {
        Calendar calendar = Calendar.getInstance();
        String date1 = CalendarOperate.formatCalender(calendar);
        calendar.add(Calendar.YEAR, -1);
        String date2 = CalendarOperate.formatCalender(calendar);
        List<PriceInfo> infos = null;
        try {
            infos = marketLogic.getPriceInfo(code, UnitType.DAY, date2, date1);
        } catch (ParameterException e) {
            e.printStackTrace();
        }
        double ave_rise = 0;
        int n = infos.size();
        for (PriceInfo info : infos) {
            ave_rise += info.rise / 100;
        }
        ave_rise = ave_rise / n;
        return NumberOpe.controlDecimalDouble(ave_rise, 2);
    }

    @Override
    public double riskProfitRate(String code) throws RemoteException, ObjectNotFoundException {
        return 0;
    }

    @Override
    public double getEnsembleAveProfitRate(String code) throws RemoteException, ObjectNotFoundException {
        Calendar calendar = Calendar.getInstance();
        String date1 = CalendarOperate.formatCalender(calendar);
        calendar.add(Calendar.YEAR, -1);
        String date2 = CalendarOperate.formatCalender(calendar);
        List<PriceInfo> infos = null;
        try {
            infos = marketLogic.getPriceInfo(code, UnitType.DAY, date2, date1);
        } catch (ParameterException e) {
            e.printStackTrace();
        }
        double ave_rise = 1;
        int n = infos.size();
        for (PriceInfo info : infos) {
            ave_rise *= (1 + info.rise / 100);
        }
        ave_rise = Math.pow(ave_rise, 1.0 / n) - 1;
        return NumberOpe.controlDecimalDouble(ave_rise, 2);
    }

    @Override
    public RiskProfitIndex getRiskProfitIndex(String code) throws RemoteException, ObjectNotFoundException {
        RiskFeatureLogic riskFeatureLogic = BLController.getRiskFeatureLogic();
        RiskProfitIndex riskProfitIndex = new RiskProfitIndex();
        riskProfitIndex.alpha = getAlpha(code);
        riskProfitIndex.beta = riskFeatureLogic.getBeta(code);
        riskProfitIndex.aveProfit = aveProfitRate(code);
        riskProfitIndex.code = code;
        riskProfitIndex.jensen = riskFeatureLogic.getJansen(code);
        riskProfitIndex.profitSd = riskFeatureLogic.getStandardDeviation(code);
        riskProfitIndex.treynor = riskFeatureLogic.getTreynor(code);
        riskProfitIndex.sharpe = riskFeatureLogic.getSharpe(code);
        riskProfitIndex.yearWaveRate = riskFeatureLogic.yearWaveRate(code);
        FundInfosEntity fundInfosEntity = baseInfoDataService.getFundInfo(code);
        riskProfitIndex.name = fundInfosEntity.getSimpleName();
        riskProfitIndex.investType = fundInfosEntity.getInvestType();
        riskProfitIndex.manageCompany = baseInfoDataService.getCompanyInfo(fundInfosEntity
                .getCompany()).getCompanyName();
        return riskProfitIndex;
    }

    @Override
    public InvestStyleAnalyse getInvestStyleAnalyse(String code) throws RemoteException, ObjectNotFoundException {
        return null;
    }
}

package com.medProject.prescriptionReaderApp;

public class MedicineObject {
    String numOfUnits, unitOfMeasure, freqPerTimeFrame, timeFrame;

    public void MedicineObject() {
        this.numOfUnits = numOfUnits;
    }

    public MedicineObject(String numOfUnits, String unitOfMeasure, String freqPerTimeFrame, String timeFrame) {
        this.numOfUnits = numOfUnits;
        this.unitOfMeasure = unitOfMeasure;
        this.freqPerTimeFrame = freqPerTimeFrame;
        this.timeFrame = timeFrame;
    }

    public String getNumOfUnits() {
        return numOfUnits;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public String getFreqPerTimeFrame() {
        return freqPerTimeFrame;
    }

    public String getTimeFrame() {
        return timeFrame;
    }

    public void setNumOfUnits(String numOfUnits) {
        this.numOfUnits = numOfUnits;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public void setFreqPerTimeFrame(String freqPerTimeFrame) {
        this.freqPerTimeFrame = freqPerTimeFrame;
    }

    public void setTimeFrame(String timeFrame) {
        this.timeFrame = timeFrame;
    }
}

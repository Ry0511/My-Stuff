package com.ry.cli;

import com.ry.converter.BasicChartFinaliser;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.util.MSDChart;
import com.ry.ffmpeg.FFMPEG;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Java interface created on 01/07/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public interface CFGOperations {
    ExecutorService getBaseRateService();
    ExecutorService getRatedService();
    FFMPEG getFfmpeg() throws RuntimeException;
    CacheDB getCacheDb() throws RuntimeException;
    File getInput() throws RuntimeException;
    File getOutput() throws RuntimeException;
    Function<EtternaNoteInfo, Stream<MSDChart>> getChartMappingFunction() throws RuntimeException;
    BasicChartFinaliser getChartFinaliser() throws RuntimeException;
}

package com.wix.rt.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.wix.nodejs.NodeRunner;
import com.wix.rt.RTProjectComponent;
import com.wix.rt.build.Result;
import com.wix.rt.build.VerifyMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RTRunner {
    private RTRunner() {
    }

    public static final String AMD = "amd";
    public static final String COMMONJS = "commonjs";
    public static final String NONE = "none";

    private static final Logger LOG = Logger.getInstance(RTRunner.class);

    private static final int TIME_OUT = (int) TimeUnit.SECONDS.toMillis(120L);

    @NotNull
    public static ProcessOutput convertFile(@NotNull RTSettings settings) throws ExecutionException {
        GeneralCommandLine commandLine = RTCliBuilder.createCommandLineLint(settings);
        return NodeRunner.execute(commandLine, TIME_OUT);
    }

    public static Result build(@NotNull String cwd, @NotNull String path, @NotNull String node, @NotNull String rtBin, @NotNull String modules) {
        RTSettings settings = RTSettings.build(cwd, node, rtBin, path, modules, false);
        Result result = new Result();
        try {
            ProcessOutput output = RTRunner.convertFile(settings);
            result.warns = parse(output.getStdout());
        } catch (ExecutionException e) {
            LOG.warn("Could not build react-templates file", e);
            RTProjectComponent.showNotification("Error running React-Templates build: " + e.getMessage() + "\ncwd: " + cwd + "\ncommand: " + rtBin, NotificationType.WARNING);
            e.printStackTrace();
        }
        return result;
    }

    //npm outdated react-templates -g -json
    public static Outdated npmOutdated(@NotNull String cwd, @NotNull String node) {
        try {
            GeneralCommandLine commandLine = NodeRunner.createCommandLine(cwd, node, "/usr/local/bin/npm");
            commandLine.addParameter("outdated");
            commandLine.addParameter("react-templates");
            commandLine.addParameter("-g");
            commandLine.addParameter("-json");
            ProcessOutput output = NodeRunner.execute(commandLine, TIME_OUT);

            String outputJSON = output.getStdout();
            return parseNpmOutdated(outputJSON);
        } catch (ExecutionException e) {
            LOG.warn("Could not build react-templates file", e);
//            RTProjectComponent.showNotification("Error running React-Templates build: " + e.getMessage() + "\ncwd: " + cwd + "\ncommand: " + rtBin, NotificationType.WARNING);
            e.printStackTrace();
        }
        return null;
    }

    public static class Outdated {
        @SerializedName("react-templates")
        public OutdatedClass rt;
    }

    public static class OutdatedClass {
        public String current;
        public String wanted;
        public String latest;
        public String location;
    }

    private static Outdated parseNpmOutdated(String json) {
        GsonBuilder builder = new GsonBuilder();
//        builder.registerTypeAdapterFactory(adapter);
        Gson g = builder.setPrettyPrinting().create();
        Type listType = new TypeToken<Outdated>() {
        }.getType();
        return g.fromJson(json, listType);
    }

    //rt --list-target-version -f json
    public static Result listVersion(@NotNull String cwd, @NotNull String node, @NotNull String rtBin) {
        RTSettings settings = RTSettings.build(cwd, node, rtBin);
        Result result = new Result();
        try {
            GeneralCommandLine commandLine = RTCliBuilder.createCommandLineLint(settings);
            commandLine.addParameter("--force");
            ProcessOutput output = NodeRunner.execute(commandLine, TIME_OUT);
            result.warns = parse(output.getStdout());
        } catch (ExecutionException e) {
            LOG.warn("Could not build react-templates file", e);
            RTProjectComponent.showNotification("Error running React-Templates build: " + e.getMessage() + "\ncwd: " + cwd + "\ncommand: " + rtBin, NotificationType.WARNING);
            e.printStackTrace();
        }
        return result;
    }

    public static Result compile(@NotNull String cwd, @NotNull String path, @NotNull String node, @NotNull String rtBin, @NotNull String modules) {
        RTSettings settings = RTSettings.build(cwd, node, rtBin, path, modules, true);
        Result result = new Result();
        try {
            GeneralCommandLine commandLine = RTCliBuilder.createCommandLineLint(settings);
            commandLine.addParameter("--force");
            ProcessOutput output = NodeRunner.execute(commandLine, TIME_OUT);
            result.warns = parse(output.getStdout());
        } catch (ExecutionException e) {
            LOG.warn("Could not build react-templates file", e);
            RTProjectComponent.showNotification("Error running React-Templates build: " + e.getMessage() + "\ncwd: " + cwd + "\ncommand: " + rtBin, NotificationType.WARNING);
            e.printStackTrace();
        }
        return result;
    }

    @NotNull
    private static ProcessOutput version(@NotNull RTSettings settings) throws ExecutionException {
        GeneralCommandLine commandLine = RTCliBuilder.version(settings);
        return NodeRunner.execute(commandLine, TIME_OUT);
    }

    @NotNull
    public static String runVersion(@NotNull RTSettings settings) throws ExecutionException {
        if (!new File(settings.rtExecutablePath).exists()) {
            LOG.warn("Calling version with invalid react-templates exe " + settings.rtExecutablePath);
            return "";
        }
        ProcessOutput out = version(settings);
        if (out.getExitCode() == 0) {
            return out.getStdout().trim();
        }
        return "";
    }

    private static List<VerifyMessage> parse(String json) {
        GsonBuilder builder = new GsonBuilder();
//        builder.registerTypeAdapterFactory(adapter);
        Gson g = builder.setPrettyPrinting().create();
        Type listType = new TypeToken<ArrayList<VerifyMessage>>() {
        }.getType();
        return g.fromJson(json, listType);
    }
}
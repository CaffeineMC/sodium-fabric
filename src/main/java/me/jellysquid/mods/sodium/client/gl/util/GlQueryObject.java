package me.jellysquid.mods.sodium.client.gl.util;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;

public class GlQueryObject extends GlHandle {
    private static boolean runningQueriedRendering = false;

    private boolean isQueryResultFetched = false;
    private int queryResultCache = -1;

    public GlQueryObject() {
        this.setHandle(GL15.glGenQueries());
    }

    public void delete() {
        GL15.glDeleteQueries(this.handle());
    }

    public void performQuery(int queryMode, Runnable renderingFunc) {
        isQueryResultFetched = false;
        queryResultCache = -1;

        GL15.glBeginQuery(queryMode, this.handle());
        runningQueriedRendering = true;

        renderingFunc.run();

        runningQueriedRendering = false;
        GL15.glEndQuery(queryMode);
    }

    public void performQueryAnySamplePassed(Runnable renderingFunc) {
        performQuery(GL33.GL_ANY_SAMPLES_PASSED, renderingFunc);
    }

    public void performQueryAnySamplePassedConservative(Runnable renderingFunc) {
        performQuery(GL43.GL_ANY_SAMPLES_PASSED_CONSERVATIVE, renderingFunc);
    }

    public void performQuerySampleNumberPassed(Runnable renderingFunc) {
        performQuery(GL15.GL_SAMPLES_PASSED, renderingFunc);
    }

    public static boolean isRunningQueriedRendering() {
        return runningQueriedRendering;
    }

    public boolean fetchQueryResultBooleanSynced() {
        return fetchQueryResultIntSynced() != 0;
    }

    public int fetchQueryResultIntSynced() {
        if (isQueryResultFetched) {
            return queryResultCache;
        }

        int result = GL15.glGetQueryObjecti(this.handle(), GL15.GL_QUERY_RESULT);

        queryResultCache = result;
        isQueryResultFetched = true;

        return result;
    }


    public void performConditionalRendering(boolean doWait, Runnable renderingFunc) {
        int mode = doWait ? GL30.GL_QUERY_BY_REGION_WAIT : GL30.GL_QUERY_BY_REGION_NO_WAIT;
        GL30.glBeginConditionalRender(this.handle(), mode);

        renderingFunc.run();

        GL30.glEndConditionalRender();
    }
}

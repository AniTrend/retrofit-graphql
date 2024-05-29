package co.anitrend.retrofit.graphql.data.arch.controller.strategy

import co.anitrend.arch.request.callback.RequestCallback

internal abstract class ControllerStrategy<D> {

    /**
     * Execute a task under an implementation strategy
     *
     * @param callback event emitter
     * @param block what will be executed
     */
    internal abstract suspend operator fun invoke(
        callback: RequestCallback,
        block: suspend () -> D?
    ): D?
}
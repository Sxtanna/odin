package com.sxtanna.odin.compile

import com.sxtanna.odin.compile.base.TokenData
import com.sxtanna.odin.compile.util.PeekIterator
import com.sxtanna.odin.runtime.Command

typealias CommandChain = MutableList<Command>

typealias IterOfTokens = PeekIterator<TokenData>
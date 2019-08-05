// Generated from com/apollographql/apollo/compiler/parser/antlr/GraphQL.g4 by ANTLR 4.7.2

package com.apollographql.apollo.compiler.parser.antlr;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class GraphQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, STRING=19, BOOLEAN=20, NAME=21, NUMBER=22, WS=23;
	public static final int
		RULE_document = 0, RULE_definition = 1, RULE_operationDefinition = 2, 
		RULE_selectionSet = 3, RULE_operationType = 4, RULE_selection = 5, RULE_field = 6, 
		RULE_fieldName = 7, RULE_alias = 8, RULE_arguments = 9, RULE_argument = 10, 
		RULE_fragmentSpread = 11, RULE_inlineFragment = 12, RULE_fragmentDefinition = 13, 
		RULE_fragmentName = 14, RULE_directives = 15, RULE_directive = 16, RULE_typeCondition = 17, 
		RULE_variableDefinitions = 18, RULE_variableDefinition = 19, RULE_variable = 20, 
		RULE_defaultValue = 21, RULE_valueOrVariable = 22, RULE_value = 23, RULE_inlineInputType = 24, 
		RULE_inlineInputTypeField = 25, RULE_type = 26, RULE_typeName = 27, RULE_listType = 28, 
		RULE_nonNullType = 29, RULE_array = 30;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "definition", "operationDefinition", "selectionSet", "operationType", 
			"selection", "field", "fieldName", "alias", "arguments", "argument", 
			"fragmentSpread", "inlineFragment", "fragmentDefinition", "fragmentName", 
			"directives", "directive", "typeCondition", "variableDefinitions", "variableDefinition", 
			"variable", "defaultValue", "valueOrVariable", "value", "inlineInputType", 
			"inlineInputTypeField", "type", "typeName", "listType", "nonNullType", 
			"array"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'{'", "','", "'}'", "'query'", "'mutation'", "'subscription'", 
			"':'", "'('", "')'", "'...'", "'on'", "'fragment'", "'@'", "'$'", "'='", 
			"'['", "']'", "'!'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, "STRING", "BOOLEAN", "NAME", 
			"NUMBER", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "GraphQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public GraphQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class DocumentContext extends ParserRuleContext {
		public List<DefinitionContext> definition() {
			return getRuleContexts(DefinitionContext.class);
		}
		public DefinitionContext definition(int i) {
			return getRuleContext(DefinitionContext.class,i);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDocument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDocument(this);
		}
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(62);
				definition();
				}
				}
				setState(65); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__11))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefinitionContext extends ParserRuleContext {
		public OperationDefinitionContext operationDefinition() {
			return getRuleContext(OperationDefinitionContext.class,0);
		}
		public FragmentDefinitionContext fragmentDefinition() {
			return getRuleContext(FragmentDefinitionContext.class,0);
		}
		public DefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefinition(this);
		}
	}

	public final DefinitionContext definition() throws RecognitionException {
		DefinitionContext _localctx = new DefinitionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_definition);
		try {
			setState(69);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__3:
			case T__4:
			case T__5:
				enterOuterAlt(_localctx, 1);
				{
				setState(67);
				operationDefinition();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(68);
				fragmentDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperationDefinitionContext extends ParserRuleContext {
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public VariableDefinitionsContext variableDefinitions() {
			return getRuleContext(VariableDefinitionsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public OperationDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationDefinition(this);
		}
	}

	public final OperationDefinitionContext operationDefinition() throws RecognitionException {
		OperationDefinitionContext _localctx = new OperationDefinitionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_operationDefinition);
		int _la;
		try {
			setState(82);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				enterOuterAlt(_localctx, 1);
				{
				setState(71);
				selectionSet();
				}
				break;
			case T__3:
			case T__4:
			case T__5:
				enterOuterAlt(_localctx, 2);
				{
				setState(72);
				operationType();
				setState(73);
				match(NAME);
				setState(75);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__7) {
					{
					setState(74);
					variableDefinitions();
					}
				}

				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__12) {
					{
					setState(77);
					directives();
					}
				}

				setState(80);
				selectionSet();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectionSetContext extends ParserRuleContext {
		public List<SelectionContext> selection() {
			return getRuleContexts(SelectionContext.class);
		}
		public SelectionContext selection(int i) {
			return getRuleContext(SelectionContext.class,i);
		}
		public SelectionSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectionSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelectionSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelectionSet(this);
		}
	}

	public final SelectionSetContext selectionSet() throws RecognitionException {
		SelectionSetContext _localctx = new SelectionSetContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_selectionSet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			match(T__0);
			setState(85);
			selection();
			setState(92);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__9) | (1L << NAME))) != 0)) {
				{
				{
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(86);
					match(T__1);
					}
				}

				setState(89);
				selection();
				}
				}
				setState(94);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(95);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OperationTypeContext extends ParserRuleContext {
		public OperationTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationType(this);
		}
	}

	public final OperationTypeContext operationType() throws RecognitionException {
		OperationTypeContext _localctx = new OperationTypeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_operationType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__4) | (1L << T__5))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectionContext extends ParserRuleContext {
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public FragmentSpreadContext fragmentSpread() {
			return getRuleContext(FragmentSpreadContext.class,0);
		}
		public InlineFragmentContext inlineFragment() {
			return getRuleContext(InlineFragmentContext.class,0);
		}
		public SelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelection(this);
		}
	}

	public final SelectionContext selection() throws RecognitionException {
		SelectionContext _localctx = new SelectionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_selection);
		try {
			setState(102);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(99);
				field();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(100);
				fragmentSpread();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(101);
				inlineFragment();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldContext extends ParserRuleContext {
		public FieldNameContext fieldName() {
			return getRuleContext(FieldNameContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitField(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_field);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(104);
			fieldName();
			setState(106);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__7) {
				{
				setState(105);
				arguments();
				}
			}

			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(108);
				directives();
				}
			}

			setState(112);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(111);
				selectionSet();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldNameContext extends ParserRuleContext {
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public FieldNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFieldName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFieldName(this);
		}
	}

	public final FieldNameContext fieldName() throws RecognitionException {
		FieldNameContext _localctx = new FieldNameContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_fieldName);
		try {
			setState(116);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(114);
				alias();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(115);
				match(NAME);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AliasContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(GraphQLParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(GraphQLParser.NAME, i);
		}
		public AliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitAlias(this);
		}
	}

	public final AliasContext alias() throws RecognitionException {
		AliasContext _localctx = new AliasContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_alias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			match(NAME);
			setState(119);
			match(T__6);
			setState(120);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentsContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArguments(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(T__7);
			setState(123);
			argument();
			setState(128);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(124);
				match(T__1);
				setState(125);
				argument();
				}
				}
				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(131);
			match(T__8);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public ValueOrVariableContext valueOrVariable() {
			return getRuleContext(ValueOrVariableContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArgument(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			match(NAME);
			setState(134);
			match(T__6);
			setState(135);
			valueOrVariable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FragmentSpreadContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentSpreadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentSpread; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentSpread(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentSpread(this);
		}
	}

	public final FragmentSpreadContext fragmentSpread() throws RecognitionException {
		FragmentSpreadContext _localctx = new FragmentSpreadContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_fragmentSpread);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			match(T__9);
			setState(138);
			fragmentName();
			setState(140);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(139);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineFragmentContext extends ParserRuleContext {
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InlineFragmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineFragment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineFragment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineFragment(this);
		}
	}

	public final InlineFragmentContext inlineFragment() throws RecognitionException {
		InlineFragmentContext _localctx = new InlineFragmentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_inlineFragment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(142);
			match(T__9);
			setState(143);
			match(T__10);
			setState(144);
			typeCondition();
			setState(146);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(145);
				directives();
				}
			}

			setState(148);
			selectionSet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FragmentDefinitionContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentDefinition(this);
		}
	}

	public final FragmentDefinitionContext fragmentDefinition() throws RecognitionException {
		FragmentDefinitionContext _localctx = new FragmentDefinitionContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_fragmentDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
			match(T__11);
			setState(151);
			fragmentName();
			setState(152);
			match(T__10);
			setState(153);
			typeCondition();
			setState(155);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(154);
				directives();
				}
			}

			setState(157);
			selectionSet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FragmentNameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public FragmentNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentName(this);
		}
	}

	public final FragmentNameContext fragmentName() throws RecognitionException {
		FragmentNameContext _localctx = new FragmentNameContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_fragmentName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(159);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectivesContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public DirectivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectives(this);
		}
	}

	public final DirectivesContext directives() throws RecognitionException {
		DirectivesContext _localctx = new DirectivesContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_directives);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(161);
				directive();
				}
				}
				setState(164); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__12 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectiveContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public ValueOrVariableContext valueOrVariable() {
			return getRuleContext(ValueOrVariableContext.class,0);
		}
		public ArgumentContext argument() {
			return getRuleContext(ArgumentContext.class,0);
		}
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirective(this);
		}
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_directive);
		try {
			setState(178);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(166);
				match(T__12);
				setState(167);
				match(NAME);
				setState(168);
				match(T__6);
				setState(169);
				valueOrVariable();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(170);
				match(T__12);
				setState(171);
				match(NAME);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(172);
				match(T__12);
				setState(173);
				match(NAME);
				setState(174);
				match(T__7);
				setState(175);
				argument();
				setState(176);
				match(T__8);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeConditionContext extends ParserRuleContext {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public TypeConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeCondition(this);
		}
	}

	public final TypeConditionContext typeCondition() throws RecognitionException {
		TypeConditionContext _localctx = new TypeConditionContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_typeCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			typeName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDefinitionsContext extends ParserRuleContext {
		public List<VariableDefinitionContext> variableDefinition() {
			return getRuleContexts(VariableDefinitionContext.class);
		}
		public VariableDefinitionContext variableDefinition(int i) {
			return getRuleContext(VariableDefinitionContext.class,i);
		}
		public VariableDefinitionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinitions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinitions(this);
		}
	}

	public final VariableDefinitionsContext variableDefinitions() throws RecognitionException {
		VariableDefinitionsContext _localctx = new VariableDefinitionsContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_variableDefinitions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			match(T__7);
			setState(183);
			variableDefinition();
			setState(188);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(184);
				match(T__1);
				setState(185);
				variableDefinition();
				}
				}
				setState(190);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(191);
			match(T__8);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDefinitionContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public VariableDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinition(this);
		}
	}

	public final VariableDefinitionContext variableDefinition() throws RecognitionException {
		VariableDefinitionContext _localctx = new VariableDefinitionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_variableDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			variable();
			setState(194);
			match(T__6);
			setState(195);
			type();
			setState(197);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__14) {
				{
				setState(196);
				defaultValue();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariable(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(199);
			match(T__13);
			setState(200);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefaultValueContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefaultValue(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(202);
			match(T__14);
			setState(203);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueOrVariableContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public ValueOrVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueOrVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterValueOrVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitValueOrVariable(this);
		}
	}

	public final ValueOrVariableContext valueOrVariable() throws RecognitionException {
		ValueOrVariableContext _localctx = new ValueOrVariableContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_valueOrVariable);
		try {
			setState(207);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__15:
			case STRING:
			case BOOLEAN:
			case NAME:
			case NUMBER:
				enterOuterAlt(_localctx, 1);
				{
				setState(205);
				value();
				}
				break;
			case T__13:
				enterOuterAlt(_localctx, 2);
				{
				setState(206);
				variable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueContext extends ParserRuleContext {
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
	 
		public ValueContext() { }
		public void copyFrom(ValueContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class StringValueContext extends ValueContext {
		public TerminalNode STRING() { return getToken(GraphQLParser.STRING, 0); }
		public StringValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterStringValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitStringValue(this);
		}
	}
	public static class BooleanValueContext extends ValueContext {
		public TerminalNode BOOLEAN() { return getToken(GraphQLParser.BOOLEAN, 0); }
		public BooleanValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitBooleanValue(this);
		}
	}
	public static class InlineInputTypeValueContext extends ValueContext {
		public InlineInputTypeContext inlineInputType() {
			return getRuleContext(InlineInputTypeContext.class,0);
		}
		public InlineInputTypeValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineInputTypeValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineInputTypeValue(this);
		}
	}
	public static class NumberValueContext extends ValueContext {
		public TerminalNode NUMBER() { return getToken(GraphQLParser.NUMBER, 0); }
		public NumberValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNumberValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNumberValue(this);
		}
	}
	public static class ArrayValueContext extends ValueContext {
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public ArrayValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArrayValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArrayValue(this);
		}
	}
	public static class LiteralValueContext extends ValueContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public LiteralValueContext(ValueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterLiteralValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitLiteralValue(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_value);
		try {
			setState(215);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				_localctx = new StringValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(209);
				match(STRING);
				}
				break;
			case NUMBER:
				_localctx = new NumberValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(210);
				match(NUMBER);
				}
				break;
			case BOOLEAN:
				_localctx = new BooleanValueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(211);
				match(BOOLEAN);
				}
				break;
			case T__15:
				_localctx = new ArrayValueContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(212);
				array();
				}
				break;
			case NAME:
				_localctx = new LiteralValueContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(213);
				match(NAME);
				}
				break;
			case T__0:
				_localctx = new InlineInputTypeValueContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(214);
				inlineInputType();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineInputTypeContext extends ParserRuleContext {
		public List<InlineInputTypeFieldContext> inlineInputTypeField() {
			return getRuleContexts(InlineInputTypeFieldContext.class);
		}
		public InlineInputTypeFieldContext inlineInputTypeField(int i) {
			return getRuleContext(InlineInputTypeFieldContext.class,i);
		}
		public InlineInputTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineInputType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineInputType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineInputType(this);
		}
	}

	public final InlineInputTypeContext inlineInputType() throws RecognitionException {
		InlineInputTypeContext _localctx = new InlineInputTypeContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_inlineInputType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			match(T__0);
			setState(218);
			inlineInputTypeField();
			setState(223);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(219);
				match(T__1);
				setState(220);
				inlineInputTypeField();
				}
				}
				setState(225);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(226);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineInputTypeFieldContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public ValueOrVariableContext valueOrVariable() {
			return getRuleContext(ValueOrVariableContext.class,0);
		}
		public InlineInputTypeFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineInputTypeField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineInputTypeField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineInputTypeField(this);
		}
	}

	public final InlineInputTypeFieldContext inlineInputTypeField() throws RecognitionException {
		InlineInputTypeFieldContext _localctx = new InlineInputTypeFieldContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_inlineInputTypeField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(228);
			match(NAME);
			setState(229);
			match(T__6);
			setState(230);
			valueOrVariable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public NonNullTypeContext nonNullType() {
			return getRuleContext(NonNullTypeContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitType(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_type);
		int _la;
		try {
			setState(240);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(232);
				typeName();
				setState(234);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(233);
					nonNullType();
					}
				}

				}
				break;
			case T__15:
				enterOuterAlt(_localctx, 2);
				{
				setState(236);
				listType();
				setState(238);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__17) {
					{
					setState(237);
					nonNullType();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeNameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public TypeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeName(this);
		}
	}

	public final TypeNameContext typeName() throws RecognitionException {
		TypeNameContext _localctx = new TypeNameContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_typeName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(242);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ListTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ListTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterListType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitListType(this);
		}
	}

	public final ListTypeContext listType() throws RecognitionException {
		ListTypeContext _localctx = new ListTypeContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_listType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(T__15);
			setState(245);
			type();
			setState(246);
			match(T__16);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NonNullTypeContext extends ParserRuleContext {
		public NonNullTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonNullType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNonNullType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNonNullType(this);
		}
	}

	public final NonNullTypeContext nonNullType() throws RecognitionException {
		NonNullTypeContext _localctx = new NonNullTypeContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_nonNullType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(T__17);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArray(this);
		}
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_array);
		int _la;
		try {
			setState(263);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(250);
				match(T__15);
				setState(251);
				value();
				setState(256);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__1) {
					{
					{
					setState(252);
					match(T__1);
					setState(253);
					value();
					}
					}
					setState(258);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(259);
				match(T__16);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(261);
				match(T__15);
				setState(262);
				match(T__16);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\31\u010c\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \3\2"+
		"\6\2B\n\2\r\2\16\2C\3\3\3\3\5\3H\n\3\3\4\3\4\3\4\3\4\5\4N\n\4\3\4\5\4"+
		"Q\n\4\3\4\3\4\5\4U\n\4\3\5\3\5\3\5\5\5Z\n\5\3\5\7\5]\n\5\f\5\16\5`\13"+
		"\5\3\5\3\5\3\6\3\6\3\7\3\7\3\7\5\7i\n\7\3\b\3\b\5\bm\n\b\3\b\5\bp\n\b"+
		"\3\b\5\bs\n\b\3\t\3\t\5\tw\n\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\7\13"+
		"\u0081\n\13\f\13\16\13\u0084\13\13\3\13\3\13\3\f\3\f\3\f\3\f\3\r\3\r\3"+
		"\r\5\r\u008f\n\r\3\16\3\16\3\16\3\16\5\16\u0095\n\16\3\16\3\16\3\17\3"+
		"\17\3\17\3\17\3\17\5\17\u009e\n\17\3\17\3\17\3\20\3\20\3\21\6\21\u00a5"+
		"\n\21\r\21\16\21\u00a6\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3"+
		"\22\3\22\3\22\5\22\u00b5\n\22\3\23\3\23\3\24\3\24\3\24\3\24\7\24\u00bd"+
		"\n\24\f\24\16\24\u00c0\13\24\3\24\3\24\3\25\3\25\3\25\3\25\5\25\u00c8"+
		"\n\25\3\26\3\26\3\26\3\27\3\27\3\27\3\30\3\30\5\30\u00d2\n\30\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\5\31\u00da\n\31\3\32\3\32\3\32\3\32\7\32\u00e0\n"+
		"\32\f\32\16\32\u00e3\13\32\3\32\3\32\3\33\3\33\3\33\3\33\3\34\3\34\5\34"+
		"\u00ed\n\34\3\34\3\34\5\34\u00f1\n\34\5\34\u00f3\n\34\3\35\3\35\3\36\3"+
		"\36\3\36\3\36\3\37\3\37\3 \3 \3 \3 \7 \u0101\n \f \16 \u0104\13 \3 \3"+
		" \3 \3 \5 \u010a\n \3 \2\2!\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \""+
		"$&(*,.\60\62\64\668:<>\2\3\3\2\6\b\2\u010e\2A\3\2\2\2\4G\3\2\2\2\6T\3"+
		"\2\2\2\bV\3\2\2\2\nc\3\2\2\2\fh\3\2\2\2\16j\3\2\2\2\20v\3\2\2\2\22x\3"+
		"\2\2\2\24|\3\2\2\2\26\u0087\3\2\2\2\30\u008b\3\2\2\2\32\u0090\3\2\2\2"+
		"\34\u0098\3\2\2\2\36\u00a1\3\2\2\2 \u00a4\3\2\2\2\"\u00b4\3\2\2\2$\u00b6"+
		"\3\2\2\2&\u00b8\3\2\2\2(\u00c3\3\2\2\2*\u00c9\3\2\2\2,\u00cc\3\2\2\2."+
		"\u00d1\3\2\2\2\60\u00d9\3\2\2\2\62\u00db\3\2\2\2\64\u00e6\3\2\2\2\66\u00f2"+
		"\3\2\2\28\u00f4\3\2\2\2:\u00f6\3\2\2\2<\u00fa\3\2\2\2>\u0109\3\2\2\2@"+
		"B\5\4\3\2A@\3\2\2\2BC\3\2\2\2CA\3\2\2\2CD\3\2\2\2D\3\3\2\2\2EH\5\6\4\2"+
		"FH\5\34\17\2GE\3\2\2\2GF\3\2\2\2H\5\3\2\2\2IU\5\b\5\2JK\5\n\6\2KM\7\27"+
		"\2\2LN\5&\24\2ML\3\2\2\2MN\3\2\2\2NP\3\2\2\2OQ\5 \21\2PO\3\2\2\2PQ\3\2"+
		"\2\2QR\3\2\2\2RS\5\b\5\2SU\3\2\2\2TI\3\2\2\2TJ\3\2\2\2U\7\3\2\2\2VW\7"+
		"\3\2\2W^\5\f\7\2XZ\7\4\2\2YX\3\2\2\2YZ\3\2\2\2Z[\3\2\2\2[]\5\f\7\2\\Y"+
		"\3\2\2\2]`\3\2\2\2^\\\3\2\2\2^_\3\2\2\2_a\3\2\2\2`^\3\2\2\2ab\7\5\2\2"+
		"b\t\3\2\2\2cd\t\2\2\2d\13\3\2\2\2ei\5\16\b\2fi\5\30\r\2gi\5\32\16\2he"+
		"\3\2\2\2hf\3\2\2\2hg\3\2\2\2i\r\3\2\2\2jl\5\20\t\2km\5\24\13\2lk\3\2\2"+
		"\2lm\3\2\2\2mo\3\2\2\2np\5 \21\2on\3\2\2\2op\3\2\2\2pr\3\2\2\2qs\5\b\5"+
		"\2rq\3\2\2\2rs\3\2\2\2s\17\3\2\2\2tw\5\22\n\2uw\7\27\2\2vt\3\2\2\2vu\3"+
		"\2\2\2w\21\3\2\2\2xy\7\27\2\2yz\7\t\2\2z{\7\27\2\2{\23\3\2\2\2|}\7\n\2"+
		"\2}\u0082\5\26\f\2~\177\7\4\2\2\177\u0081\5\26\f\2\u0080~\3\2\2\2\u0081"+
		"\u0084\3\2\2\2\u0082\u0080\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0085\3\2"+
		"\2\2\u0084\u0082\3\2\2\2\u0085\u0086\7\13\2\2\u0086\25\3\2\2\2\u0087\u0088"+
		"\7\27\2\2\u0088\u0089\7\t\2\2\u0089\u008a\5.\30\2\u008a\27\3\2\2\2\u008b"+
		"\u008c\7\f\2\2\u008c\u008e\5\36\20\2\u008d\u008f\5 \21\2\u008e\u008d\3"+
		"\2\2\2\u008e\u008f\3\2\2\2\u008f\31\3\2\2\2\u0090\u0091\7\f\2\2\u0091"+
		"\u0092\7\r\2\2\u0092\u0094\5$\23\2\u0093\u0095\5 \21\2\u0094\u0093\3\2"+
		"\2\2\u0094\u0095\3\2\2\2\u0095\u0096\3\2\2\2\u0096\u0097\5\b\5\2\u0097"+
		"\33\3\2\2\2\u0098\u0099\7\16\2\2\u0099\u009a\5\36\20\2\u009a\u009b\7\r"+
		"\2\2\u009b\u009d\5$\23\2\u009c\u009e\5 \21\2\u009d\u009c\3\2\2\2\u009d"+
		"\u009e\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u00a0\5\b\5\2\u00a0\35\3\2\2"+
		"\2\u00a1\u00a2\7\27\2\2\u00a2\37\3\2\2\2\u00a3\u00a5\5\"\22\2\u00a4\u00a3"+
		"\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7"+
		"!\3\2\2\2\u00a8\u00a9\7\17\2\2\u00a9\u00aa\7\27\2\2\u00aa\u00ab\7\t\2"+
		"\2\u00ab\u00b5\5.\30\2\u00ac\u00ad\7\17\2\2\u00ad\u00b5\7\27\2\2\u00ae"+
		"\u00af\7\17\2\2\u00af\u00b0\7\27\2\2\u00b0\u00b1\7\n\2\2\u00b1\u00b2\5"+
		"\26\f\2\u00b2\u00b3\7\13\2\2\u00b3\u00b5\3\2\2\2\u00b4\u00a8\3\2\2\2\u00b4"+
		"\u00ac\3\2\2\2\u00b4\u00ae\3\2\2\2\u00b5#\3\2\2\2\u00b6\u00b7\58\35\2"+
		"\u00b7%\3\2\2\2\u00b8\u00b9\7\n\2\2\u00b9\u00be\5(\25\2\u00ba\u00bb\7"+
		"\4\2\2\u00bb\u00bd\5(\25\2\u00bc\u00ba\3\2\2\2\u00bd\u00c0\3\2\2\2\u00be"+
		"\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf\u00c1\3\2\2\2\u00c0\u00be\3\2"+
		"\2\2\u00c1\u00c2\7\13\2\2\u00c2\'\3\2\2\2\u00c3\u00c4\5*\26\2\u00c4\u00c5"+
		"\7\t\2\2\u00c5\u00c7\5\66\34\2\u00c6\u00c8\5,\27\2\u00c7\u00c6\3\2\2\2"+
		"\u00c7\u00c8\3\2\2\2\u00c8)\3\2\2\2\u00c9\u00ca\7\20\2\2\u00ca\u00cb\7"+
		"\27\2\2\u00cb+\3\2\2\2\u00cc\u00cd\7\21\2\2\u00cd\u00ce\5\60\31\2\u00ce"+
		"-\3\2\2\2\u00cf\u00d2\5\60\31\2\u00d0\u00d2\5*\26\2\u00d1\u00cf\3\2\2"+
		"\2\u00d1\u00d0\3\2\2\2\u00d2/\3\2\2\2\u00d3\u00da\7\25\2\2\u00d4\u00da"+
		"\7\30\2\2\u00d5\u00da\7\26\2\2\u00d6\u00da\5> \2\u00d7\u00da\7\27\2\2"+
		"\u00d8\u00da\5\62\32\2\u00d9\u00d3\3\2\2\2\u00d9\u00d4\3\2\2\2\u00d9\u00d5"+
		"\3\2\2\2\u00d9\u00d6\3\2\2\2\u00d9\u00d7\3\2\2\2\u00d9\u00d8\3\2\2\2\u00da"+
		"\61\3\2\2\2\u00db\u00dc\7\3\2\2\u00dc\u00e1\5\64\33\2\u00dd\u00de\7\4"+
		"\2\2\u00de\u00e0\5\64\33\2\u00df\u00dd\3\2\2\2\u00e0\u00e3\3\2\2\2\u00e1"+
		"\u00df\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e4\3\2\2\2\u00e3\u00e1\3\2"+
		"\2\2\u00e4\u00e5\7\5\2\2\u00e5\63\3\2\2\2\u00e6\u00e7\7\27\2\2\u00e7\u00e8"+
		"\7\t\2\2\u00e8\u00e9\5.\30\2\u00e9\65\3\2\2\2\u00ea\u00ec\58\35\2\u00eb"+
		"\u00ed\5<\37\2\u00ec\u00eb\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00f3\3\2"+
		"\2\2\u00ee\u00f0\5:\36\2\u00ef\u00f1\5<\37\2\u00f0\u00ef\3\2\2\2\u00f0"+
		"\u00f1\3\2\2\2\u00f1\u00f3\3\2\2\2\u00f2\u00ea\3\2\2\2\u00f2\u00ee\3\2"+
		"\2\2\u00f3\67\3\2\2\2\u00f4\u00f5\7\27\2\2\u00f59\3\2\2\2\u00f6\u00f7"+
		"\7\22\2\2\u00f7\u00f8\5\66\34\2\u00f8\u00f9\7\23\2\2\u00f9;\3\2\2\2\u00fa"+
		"\u00fb\7\24\2\2\u00fb=\3\2\2\2\u00fc\u00fd\7\22\2\2\u00fd\u0102\5\60\31"+
		"\2\u00fe\u00ff\7\4\2\2\u00ff\u0101\5\60\31\2\u0100\u00fe\3\2\2\2\u0101"+
		"\u0104\3\2\2\2\u0102\u0100\3\2\2\2\u0102\u0103\3\2\2\2\u0103\u0105\3\2"+
		"\2\2\u0104\u0102\3\2\2\2\u0105\u0106\7\23\2\2\u0106\u010a\3\2\2\2\u0107"+
		"\u0108\7\22\2\2\u0108\u010a\7\23\2\2\u0109\u00fc\3\2\2\2\u0109\u0107\3"+
		"\2\2\2\u010a?\3\2\2\2\36CGMPTY^hlorv\u0082\u008e\u0094\u009d\u00a6\u00b4"+
		"\u00be\u00c7\u00d1\u00d9\u00e1\u00ec\u00f0\u00f2\u0102\u0109";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
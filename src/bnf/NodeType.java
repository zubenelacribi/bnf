package bnf;

public enum NodeType {
	token,
	identifier,
	sequence,
	choice,
	token_keyword,
	identifier_keyword,
	new_line_keyword,
	optional,
	repetition
}

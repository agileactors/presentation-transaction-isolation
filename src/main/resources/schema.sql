CREATE TABLE ser_sample (
	id int8 NOT NULL,
	indexed_column varchar NOT NULL,
	non_indexed_column varchar NOT NULL,
	marble_colour varchar NOT NULL,
	CONSTRAINT ser_sample_pk PRIMARY KEY (id)
);
CREATE INDEX ser_sample_indexed_column_idx ON public.ser_sample USING btree (indexed_column);
CREATE INDEX ser_sample_marble_colour_idx ON public.ser_sample USING btree (marble_colour);

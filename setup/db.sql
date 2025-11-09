CREATE TABLE files (
    id serial primary key,
    annotation text,
    content text,
    date bigint,
    location_coordinates text[],
    location_address text[],
    location_buildingname text[],
    path text,
    tags integer[],
    uuid text,
	journal_id integer
);

CREATE INDEX journal_index on files(journal_id);

CREATE INDEX date_index on files(date);

CREATE TABLE tags (
    id serial primary key,
    name text,
    parent int DEFAULT -1, 
    full_name text, 
    is_folder boolean DEFAULT false
);

CREATE INDEX tags_folder on tags(folder);
CREATE INDEX tags_name on tags(name);
CREATE INDEX tags_id on tags(id);

CREATE TABLE backlinks(
    id serial primary key,
    origin INTEGER,
    target INTEGER, annotation text, display boolean
);

CREATE INDEX backlinks_origin ON backlinks (origin);
CREATE INDEX backlinks_target ON backlinks (target);

CREATE TABLE events (
    id serial primary key,
    name text,
    description text,
    parent integer,
    is_folder boolean
);

CREATE INDEX events_parent ON events(parent);

CREATE TABLE events_file (
    id serial primary key,
    file integer,
    event integer
);

CREATE INDEX events_file_file ON events_file(file);
CREATE INDEX events_file_event ON events_file(event);


CREATE TABLE journals (
	id serial primary key,
	name text,
	description text,
    color integer
);

INSERT INTO journals(name) VALUES ('Default Journal');
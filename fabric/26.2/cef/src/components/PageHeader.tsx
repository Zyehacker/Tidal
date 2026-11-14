/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

import { Icon } from "../icons";

interface PageHeaderProps {
  title: string;
  search: string;
  onSearch: (value: string) => void;
  onClose: () => void;
}

export function PageHeader({ title, search, onSearch, onClose }: PageHeaderProps) {
  return (
    <header className="page-header">
      <h1>{title}</h1>
      <label className="search-field">
        <Icon name="search" size={15} />
        <input
          value={search}
          onChange={(event) => onSearch(event.target.value)}
          placeholder={`Search ${title}`}
          aria-label={`Search ${title}`}
        />
      </label>
      <button className="icon-button close-button" aria-label="Close Tidal" onClick={onClose}>
        <Icon name="x" />
      </button>
    </header>
  );
}

import { initApi, loadRemoteTracks, loadBundledTracks, updateRemote, updateBundled, insertRemote, publicAudioUrl, publicThumbnailUrl, thumbnailPathForAudio, uploadAudio, uploadImage } from './api';
import type { BundledTrack, RemoteTrack, TrackKind } from './types';

const REMOTE_ID_FLOOR = 100;
const app = document.getElementById('app')!;

let remoteTracks: RemoteTrack[] = [];
let bundledTracks: BundledTrack[] = [];
let remoteTbody: HTMLTableSectionElement | null = null;
let bundledTbody: HTMLTableSectionElement | null = null;
let addForm: HTMLFormElement | null = null;

function showError(err: unknown) {
  const box = document.createElement('div');
  box.className = 'error';
  box.textContent = `Error: ${err instanceof Error ? err.message : String(err)}`;
  app.prepend(box);
}

type ElProps<K extends keyof HTMLElementTagNameMap> = Partial<HTMLElementTagNameMap[K]> & {
  dataset?: Record<string, string>;
};

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  props: ElProps<K> = {},
  children: (Node | string)[] = [],
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  const { dataset, ...rest } = props;
  Object.assign(node, rest);
  if (dataset) {
    for (const [key, value] of Object.entries(dataset)) {
      node.dataset[key] = value;
    }
  }
  for (const c of children) node.append(c);
  return node;
}

// ── Remote tracks ────────────────────────────────────────────────────────────

async function nextRemoteId(tracks: RemoteTrack[]): Promise<number> {
  const max = tracks.reduce((m, t) => Math.max(m, t.id), REMOTE_ID_FLOOR - 1);
  return Math.max(max + 1, REMOTE_ID_FLOOR);
}

function folderFor(kind: TrackKind): string {
  return kind === 'music' ? 'lofi' : 'ambience';
}

function remoteTrack(id: number): RemoteTrack {
  const track = remoteTracks.find((t) => t.id === id);
  if (!track) throw new Error(`Unknown remote track id ${id}`);
  return track;
}

function replaceRemoteRow(track: RemoteTrack) {
  const idx = remoteTracks.findIndex((t) => t.id === track.id);
  if (idx >= 0) remoteTracks[idx] = track;
  const existing = remoteTbody?.querySelector(`tr[data-remote-id="${track.id}"]`);
  const row = buildRemoteRow(track);
  if (existing) {
    existing.replaceWith(row);
  } else {
    remoteTbody?.append(row);
  }
}

async function addRemoteTrack(form: HTMLFormElement, existing: RemoteTrack[]) {
  const fd = new FormData(form);
  const kind = fd.get('kind') as TrackKind;
  const file = fd.get('file') as File;
  if (!file || file.size === 0) throw new Error('Choose an MP3 file');
  const filePath = `${folderFor(kind)}/${file.name}`;
  const thumbnailPath = thumbnailPathForAudio(filePath);
  const thumbFile = fd.get('thumbnail') as File;

  await uploadAudio(filePath, file);
  if (thumbFile && thumbFile.size > 0) {
    await uploadImage(thumbnailPath, thumbFile);
  }

  const id = await nextRemoteId(existing);
  const row: RemoteTrack = {
    id,
    kind,
    name: String(fd.get('name') ?? ''),
    artist: String(fd.get('artist') ?? ''),
    symbol: String(fd.get('symbol') ?? ''),
    file_path: filePath,
    thumbnail_path: thumbnailPath,
    file_size_bytes: file.size,
    is_premium: fd.get('is_premium') === 'on',
    sort_order: Number(fd.get('sort_order') ?? 0),
    is_active: true,
    updated_at: new Date().toISOString(),
  };
  await insertRemote(row);

  remoteTracks = [...remoteTracks, row].sort((a, b) => a.sort_order - b.sort_order || a.id - b.id);
  replaceRemoteRow(row);
  form.reset();
}

function thumbnailPreviewCell(track: RemoteTrack, bust = ''): HTMLTableCellElement {
  const path = track.thumbnail_path || thumbnailPathForAudio(track.file_path);
  const td = el('td', { className: 'thumb-cell' });
  const wrap = el('div', { className: 'thumb-preview' });
  const img = el('img', {
    alt: track.name,
    title: path,
    src: `${publicThumbnailUrl(path)}${bust}`,
  }) as HTMLImageElement;
  img.addEventListener('error', () => {
    img.replaceWith(el('span', { className: 'thumb-missing', textContent: 'missing' }));
  });
  wrap.append(img);
  td.append(wrap, el('div', { className: 'hint thumb-path', textContent: path }));
  return td;
}

function thumbnailReplaceCell(track: RemoteTrack): HTMLTableCellElement {
  const path = track.thumbnail_path || thumbnailPathForAudio(track.file_path);
  const input = el('input', {
    type: 'file',
    accept: 'image/webp,image/png',
  }) as HTMLInputElement;
  const btn = el('button', { textContent: 'Upload thumb' });
  btn.addEventListener('click', async () => {
    const file = input.files?.[0];
    if (!file) {
      showError(new Error('Choose a WebP or PNG thumbnail first'));
      return;
    }
    btn.disabled = true;
    try {
      await uploadImage(path, file);
      let updated = remoteTrack(track.id);
      if (updated.thumbnail_path !== path) {
        await updateRemote(track.id, {
          thumbnail_path: path,
          updated_at: new Date().toISOString(),
        });
        updated = { ...updated, thumbnail_path: path };
      }
      replaceRemoteRow(updated);
      input.value = '';
    } catch (err) {
      showError(err);
    } finally {
      btn.disabled = false;
    }
  });
  return el('td', { className: 'thumb-upload-cell' }, [input, btn]);
}

function buildRemoteRow(track: RemoteTrack): HTMLTableRowElement {
  return el('tr', {
    className: track.is_active ? '' : 'inactive',
    dataset: { remoteId: String(track.id) },
  }, [
    el('td', { textContent: String(track.id) }),
    thumbnailPreviewCell(track),
    el('td', { textContent: track.kind }),
    el('td', { textContent: track.name }),
    el('td', {}, [toggleBtn(track.is_active ? 'active' : 'hidden', async () => {
      const current = remoteTrack(track.id);
      const is_active = !current.is_active;
      await updateRemote(track.id, { is_active, updated_at: new Date().toISOString() });
      replaceRemoteRow({ ...current, is_active });
    })]),
    el('td', {}, [toggleBtn(track.is_premium ? 'premium' : 'free', async () => {
      const current = remoteTrack(track.id);
      const is_premium = !current.is_premium;
      await updateRemote(track.id, { is_premium, updated_at: new Date().toISOString() });
      replaceRemoteRow({ ...current, is_premium });
    })]),
    el('td', { textContent: String(track.sort_order) }),
    el('td', { className: 'hint', textContent: track.file_path }),
    el('td', {}, [el('audio', { controls: true, src: publicAudioUrl(track.file_path) })]),
    thumbnailReplaceCell(track),
  ]);
}

function renderRemotePanel(container: HTMLElement) {
  remoteTbody = el('tbody', {}, remoteTracks.map(buildRemoteRow));

  const table = el('table', { className: 'remote-table' }, [
    el('thead', {}, [
      el('tr', {}, [
        'id',
        'thumbnail',
        'kind',
        'name',
        'visible',
        'lock',
        'sort',
        'file',
        'audio',
        'replace thumb',
      ].map((h) => el('th', { textContent: h }))),
    ]),
    remoteTbody,
  ]);

  addForm = el('form', { className: 'row-form' });
  addForm.append(
    selectField('kind', ['ambience', 'music']),
    inputField('name', 'name'),
    inputField('artist', 'artist'),
    inputField('symbol', 'symbol'),
    inputField('sort_order', 'sort', 'number'),
    checkboxField('is_premium', 'premium'),
    el('label', { className: 'file-label' }, [
      ' MP3 ',
      el('input', { type: 'file', name: 'file', accept: 'audio/mpeg' }),
    ]),
    el('label', { className: 'file-label' }, [
      ' thumb (optional) ',
      el('input', { type: 'file', name: 'thumbnail', accept: 'image/webp,image/png' }),
    ]),
    el('button', { className: 'primary', type: 'submit', textContent: 'Upload + add' }),
  );
  addForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await addRemoteTrack(addForm!, remoteTracks);
    } catch (err) {
      showError(err);
    }
  });

  container.append(
    el('h2', { textContent: 'Remote tracks (soundscape_tracks)' }),
    el('p', {
      className: 'hint',
      textContent:
        'Catalog in Supabase; MP3 + WebP thumbnails on Cloudflare R2. Thumbnail path defaults to the MP3 stem with .webp.',
    }),
    addForm,
    table,
  );
}

// ── Bundled (APK) tracks ───────────────────────────────────────────────────────

function bundledTrack(kind: TrackKind, localId: number): BundledTrack {
  const track = bundledTracks.find((t) => t.kind === kind && t.local_id === localId);
  if (!track) throw new Error(`Unknown bundled track ${kind}/${localId}`);
  return track;
}

function replaceBundledRow(track: BundledTrack) {
  const idx = bundledTracks.findIndex((t) => t.kind === track.kind && t.local_id === track.local_id);
  if (idx >= 0) bundledTracks[idx] = track;
  const key = `${track.kind}-${track.local_id}`;
  const existing = bundledTbody?.querySelector(`tr[data-bundled-key="${key}"]`);
  const row = buildBundledRow(track);
  if (existing) {
    existing.replaceWith(row);
  } else {
    bundledTbody?.append(row);
  }
}

function sortEditor(t: BundledTrack): HTMLElement {
  const input = el('input', { type: 'number', value: String(t.sort_order) });
  input.style.width = '64px';
  input.addEventListener('change', async () => {
    const sort_order = Number(input.value);
    try {
      await updateBundled(t.kind, t.local_id, { sort_order });
      const current = bundledTrack(t.kind, t.local_id);
      bundledTracks[idxOfBundled(current)] = { ...current, sort_order };
    } catch (err) {
      showError(err);
      input.value = String(bundledTrack(t.kind, t.local_id).sort_order);
    }
  });
  return input;
}

function idxOfBundled(track: BundledTrack): number {
  return bundledTracks.findIndex((t) => t.kind === track.kind && t.local_id === track.local_id);
}

function buildBundledRow(track: BundledTrack): HTMLTableRowElement {
  return el('tr', {
    className: track.is_active ? '' : 'inactive',
    dataset: { bundledKey: `${track.kind}-${track.local_id}` },
  }, [
    el('td', { textContent: track.kind }),
    el('td', { textContent: String(track.local_id) }),
    el('td', { textContent: track.raw_resource }),
    el('td', {}, [toggleBtn(track.is_active ? 'active' : 'hidden', async () => {
      const current = bundledTrack(track.kind, track.local_id);
      const is_active = !current.is_active;
      await updateBundled(track.kind, track.local_id, { is_active });
      replaceBundledRow({ ...current, is_active });
    })]),
    el('td', {}, [toggleBtn(track.is_premium ? 'premium' : 'free', async () => {
      const current = bundledTrack(track.kind, track.local_id);
      const is_premium = !current.is_premium;
      await updateBundled(track.kind, track.local_id, { is_premium });
      replaceBundledRow({ ...current, is_premium });
    })]),
    el('td', {}, [sortEditor(track)]),
    el('td', { className: 'hint', textContent: 'APK bundled — bytes not managed here' }),
  ]);
}

function renderBundledPanel(container: HTMLElement) {
  bundledTbody = el('tbody', {}, bundledTracks.map(buildBundledRow));

  const table = el('table', {}, [
    el('thead', {}, [
      el('tr', {}, ['kind', 'local_id', 'raw_resource', 'visible', 'lock', 'sort', 'notes'].map((h) =>
        el('th', { textContent: h }),
      )),
    ]),
    bundledTbody,
  ]);

  container.append(
    el('h2', { textContent: 'Local tracks (soundscape_bundled_catalog)' }),
    el('p', { className: 'hint', textContent: 'Audio ships in the APK. Edit visibility / premium / order only.' }),
    table,
  );
}

// ── Shared widgets ──────────────────────────────────────────────────────────

function toggleBtn(label: string, onClick: () => Promise<void>): HTMLButtonElement {
  const btn = el('button', { textContent: label });
  btn.addEventListener('click', async () => {
    btn.disabled = true;
    try {
      await onClick();
    } catch (err) {
      showError(err);
    } finally {
      btn.disabled = false;
    }
  });
  return btn;
}

function inputField(name: string, placeholder: string, type = 'text'): HTMLInputElement {
  return el('input', { name, placeholder, type });
}

function checkboxField(name: string, label: string): HTMLLabelElement {
  return el('label', {}, [el('input', { type: 'checkbox', name }), ` ${label}`]);
}

function selectField(name: string, options: string[]): HTMLSelectElement {
  return el('select', { name }, options.map((o) => el('option', { value: o, textContent: o })));
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────

async function bootstrap() {
  app.replaceChildren(el('p', { className: 'muted', textContent: 'Loading…' }));
  try {
    await initApi();
    [remoteTracks, bundledTracks] = await Promise.all([loadRemoteTracks(), loadBundledTracks()]);
    app.replaceChildren();
    const remoteSection = el('section');
    const bundledSection = el('section');
    renderRemotePanel(remoteSection);
    renderBundledPanel(bundledSection);
    app.append(remoteSection, bundledSection);
  } catch (err) {
    app.replaceChildren();
    showError(err);
  }
}

bootstrap();

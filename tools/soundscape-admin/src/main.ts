import { supabase } from './supabase';
import { publicAudioUrl, uploadAudio } from './r2';
import type { BundledTrack, RemoteTrack, TrackKind } from './types';

const REMOTE_ID_FLOOR = 100;
const app = document.getElementById('app')!;

function showError(err: unknown) {
  const box = document.createElement('div');
  box.className = 'error';
  box.textContent = `Error: ${err instanceof Error ? err.message : String(err)}`;
  app.prepend(box);
}

function el<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  props: Partial<HTMLElementTagNameMap[K]> = {},
  children: (Node | string)[] = [],
): HTMLElementTagNameMap[K] {
  const node = document.createElement(tag);
  Object.assign(node, props);
  for (const c of children) node.append(c);
  return node;
}

// ── Remote tracks ────────────────────────────────────────────────────────────

async function loadRemoteTracks(): Promise<RemoteTrack[]> {
  const { data, error } = await supabase
    .from('soundscape_tracks')
    .select('*')
    .order('sort_order');
  if (error) throw error;
  return (data ?? []) as RemoteTrack[];
}

async function nextRemoteId(tracks: RemoteTrack[]): Promise<number> {
  const max = tracks.reduce((m, t) => Math.max(m, t.id), REMOTE_ID_FLOOR - 1);
  return Math.max(max + 1, REMOTE_ID_FLOOR);
}

async function updateRemote(id: number, patch: Partial<RemoteTrack>) {
  const { error } = await supabase.from('soundscape_tracks').update(patch).eq('id', id);
  if (error) throw error;
}

function folderFor(kind: TrackKind): string {
  return kind === 'music' ? 'lofi' : 'ambience';
}

async function addRemoteTrack(form: HTMLFormElement, existing: RemoteTrack[]) {
  const fd = new FormData(form);
  const kind = fd.get('kind') as TrackKind;
  const file = fd.get('file') as File;
  if (!file || file.size === 0) throw new Error('Choose an MP3 file');
  const filePath = `${folderFor(kind)}/${file.name}`;

  await uploadAudio(filePath, file);

  const id = await nextRemoteId(existing);
  const { error: insErr } = await supabase.from('soundscape_tracks').insert({
    id,
    kind,
    name: String(fd.get('name') ?? ''),
    artist: String(fd.get('artist') ?? ''),
    symbol: String(fd.get('symbol') ?? ''),
    file_path: filePath,
    file_size_bytes: file.size,
    is_premium: fd.get('is_premium') === 'on',
    sort_order: Number(fd.get('sort_order') ?? 0),
    is_active: true,
    updated_at: new Date().toISOString(),
  });
  if (insErr) throw insErr;
}

function renderRemotePanel(container: HTMLElement, tracks: RemoteTrack[]) {
  const rows = tracks.map((t) =>
    el('tr', { className: t.is_active ? '' : 'inactive' }, [
      el('td', { textContent: String(t.id) }),
      el('td', { textContent: t.kind }),
      el('td', { textContent: t.name }),
      el('td', {}, [toggleBtn(t.is_active ? 'active' : 'hidden', async () => {
        await updateRemote(t.id, { is_active: !t.is_active, updated_at: new Date().toISOString() });
        await refresh();
      })]),
      el('td', {}, [toggleBtn(t.is_premium ? 'premium' : 'free', async () => {
        await updateRemote(t.id, { is_premium: !t.is_premium, updated_at: new Date().toISOString() });
        await refresh();
      })]),
      el('td', { textContent: String(t.sort_order) }),
      el('td', { textContent: t.file_path }),
      el('td', {}, [el('audio', { controls: true, src: publicAudioUrl(t.file_path) })]),
    ]),
  );

  const table = el('table', {}, [
    el('thead', {}, [
      el('tr', {}, ['id', 'kind', 'name', 'visible', 'lock', 'sort', 'file', 'preview'].map((h) =>
        el('th', { textContent: h }),
      )),
    ]),
    el('tbody', {}, rows),
  ]);

  const form = el('form', { className: 'row-form' });
  form.append(
    selectField('kind', ['ambience', 'music']),
    inputField('name', 'name'),
    inputField('artist', 'artist'),
    inputField('symbol', 'symbol'),
    inputField('sort_order', 'sort', 'number'),
    checkboxField('is_premium', 'premium'),
    el('input', { type: 'file', name: 'file', accept: 'audio/mpeg' }),
    el('button', { className: 'primary', type: 'submit', textContent: 'Upload + add' }),
  );
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await addRemoteTrack(form, tracks);
      await refresh();
    } catch (err) {
      showError(err);
    }
  });

  container.append(
    el('h2', { textContent: 'Remote tracks (soundscape_tracks)' }),
    el('p', { className: 'hint', textContent: 'Catalog in Supabase; MP3 bytes on Cloudflare R2. Soft-delete via the visible toggle.' }),
    form,
    table,
  );
}

// ── Bundled (APK) tracks ───────────────────────────────────────────────────────

async function loadBundledTracks(): Promise<BundledTrack[]> {
  const { data, error } = await supabase
    .from('soundscape_bundled_catalog')
    .select('*')
    .order('sort_order');
  if (error) throw error;
  return (data ?? []) as BundledTrack[];
}

async function updateBundled(kind: TrackKind, localId: number, patch: Partial<BundledTrack>) {
  const { error } = await supabase
    .from('soundscape_bundled_catalog')
    .update(patch)
    .eq('kind', kind)
    .eq('local_id', localId);
  if (error) throw error;
}

function renderBundledPanel(container: HTMLElement, tracks: BundledTrack[]) {
  const rows = tracks.map((t) =>
    el('tr', { className: t.is_active ? '' : 'inactive' }, [
      el('td', { textContent: t.kind }),
      el('td', { textContent: String(t.local_id) }),
      el('td', { textContent: t.raw_resource }),
      el('td', {}, [toggleBtn(t.is_active ? 'active' : 'hidden', async () => {
        await updateBundled(t.kind, t.local_id, { is_active: !t.is_active });
        await refresh();
      })]),
      el('td', {}, [toggleBtn(t.is_premium ? 'premium' : 'free', async () => {
        await updateBundled(t.kind, t.local_id, { is_premium: !t.is_premium });
        await refresh();
      })]),
      el('td', {}, [sortEditor(t)]),
      el('td', { className: 'hint', textContent: 'APK bundled — bytes not managed here' }),
    ]),
  );

  const table = el('table', {}, [
    el('thead', {}, [
      el('tr', {}, ['kind', 'local_id', 'raw_resource', 'visible', 'lock', 'sort', 'notes'].map((h) =>
        el('th', { textContent: h }),
      )),
    ]),
    el('tbody', {}, rows),
  ]);

  container.append(
    el('h2', { textContent: 'Local tracks (soundscape_bundled_catalog)' }),
    el('p', { className: 'hint', textContent: 'Audio ships in the APK. Edit visibility / premium / order only.' }),
    table,
  );
}

function sortEditor(t: BundledTrack): HTMLElement {
  const input = el('input', { type: 'number', value: String(t.sort_order) });
  input.style.width = '64px';
  input.addEventListener('change', async () => {
    try {
      await updateBundled(t.kind, t.local_id, { sort_order: Number(input.value) });
      await refresh();
    } catch (err) {
      showError(err);
    }
  });
  return input;
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

async function refresh() {
  app.replaceChildren();
  try {
    const [remote, bundled] = await Promise.all([loadRemoteTracks(), loadBundledTracks()]);
    const remoteSection = el('section');
    const bundledSection = el('section');
    renderRemotePanel(remoteSection, remote);
    renderBundledPanel(bundledSection, bundled);
    app.append(remoteSection, bundledSection);
  } catch (err) {
    showError(err);
  }
}

refresh();

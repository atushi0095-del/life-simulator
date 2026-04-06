import { v4 as uuidv4 } from 'uuid';

/** Generate a URL-safe share ID */
export function generateShareId(): string {
  return uuidv4().replace(/-/g, '');
}

import { createContext, useContext, type ReactNode } from 'react';
import { CssBaseline } from '@mui/material';
import { ThemeProvider, useColorScheme } from '@mui/material/styles';
import { theme } from '../theme';

interface ColorModeContextValue {
  mode: 'light' | 'dark';
  toggleMode: () => void;
}

const ColorModeContext = createContext<ColorModeContextValue | null>(null);

function ColorModeBridge({ children }: { children: ReactNode }) {
  const { mode, systemMode, setMode } = useColorScheme();
  const currentMode = mode === 'system' ? systemMode : mode;
  const toggleMode = () => {
    const nextMode = currentMode === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = nextMode;
    setMode(nextMode);
  };

  if (currentMode !== 'light' && currentMode !== 'dark') return null;

  return (
    <ColorModeContext.Provider
      value={{
        mode: currentMode,
        toggleMode,
      }}
    >
      {children}
    </ColorModeContext.Provider>
  );
}

export function ColorModeProvider({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider
      theme={theme}
      defaultMode="system"
      modeStorageKey="ptu-social-color-mode"
      noSsr
      disableTransitionOnChange
    >
      <CssBaseline />
      <ColorModeBridge>
        {children}
      </ColorModeBridge>
    </ThemeProvider>
  );
}

export function useColorMode(): ColorModeContextValue {
  const context = useContext(ColorModeContext);
  if (context === null) {
    throw new Error('useColorMode must be used within ColorModeProvider');
  }
  return context;
}
